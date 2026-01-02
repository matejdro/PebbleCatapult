package com.matejdro.pebble.bluetooth.common

import com.matejdro.pebble.bluetooth.common.exceptions.UnrecoverableWatchTransferException
import io.rebble.pebblekit2.client.PebbleSender
import io.rebble.pebblekit2.common.model.PebbleDictionary
import io.rebble.pebblekit2.common.model.TransmissionResult
import io.rebble.pebblekit2.common.model.WatchIdentifier
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import logcat.logcat
import java.util.PriorityQueue
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds

class PacketQueue(
   private val sender: PebbleSender,
   private val watch: WatchIdentifier,
   private val watchappUuid: UUID,
) {
   private val newPacketNotification = Channel<Unit>(Channel.CONFLATED)

   private val queue = PriorityQueue<Packet>()

   /**
    * (Eventually) send a packet to the watch.
    *
    * This will insert the packet into the queue and suspend until the packet is sent.
    *
    * If this coroutine is cancelled before the packet is sent, the packet will be removed from the queue.
    */
   suspend fun sendPacket(dictionary: PebbleDictionary, priority: Int = 0) {
      logcat { "Enqueue packet(id = ${dictionary[0u]}, priority = $priority)" }

      val sentNofification = CompletableDeferred<Unit>()
      val packet = Packet(dictionary, priority, sentNofification)

      synchronized(queue) {
         queue.add(packet)
      }

      newPacketNotification.send(Unit)
      try {
         sentNofification.await()
      } catch (e: CancellationException) {
         synchronized(queue) {
            queue.remove(packet)
         }
         throw e
      }
   }

   /**
    * Process packets in this queue. This will suspend indefinitely.
    */
   suspend fun runQueue(): Nothing {
      while (true) {
         val nextPacket = synchronized(queue) {
            queue.poll()
         }

         if (nextPacket == null) {
            newPacketNotification.receive()
            continue
         }

         logcat { "Sending packet(id = ${nextPacket.dictionary[0u]})" }
         sendPacket(nextPacket)
      }
   }

   private suspend fun sendPacket(packet: Packet) {
      var nextRetryDelay = START_RETRY_DELAY
      do {
         val result = sender.sendDataToPebble(watchappUuid, packet.dictionary, listOf(watch))
         if (result == null) {
            packet.sentNofification.completeExceptionally(
               UnrecoverableWatchTransferException("No Pebble app is installed")
            )
            break
         }

         val watchResult = result[watch]
         val retry = when (watchResult) {
            TransmissionResult.Success -> {
               logcat { "Sent" }
               packet.sentNofification.complete(Unit)
               false
            }

            TransmissionResult.FailedTimeout,
            TransmissionResult.FailedWatchNotConnected,
            TransmissionResult.FailedWatchNacked,
            -> {
               logcat { "Sending failed ($watchResult). Retrying..." }
               delay(nextRetryDelay)
               nextRetryDelay *= 2
               true
            }

            TransmissionResult.FailedNoPermissions,
            is TransmissionResult.Unknown,
            null,
            -> {
               logcat { "Sending failed unrecoverably (${watchResult ?: "null"})" }
               packet.sentNofification.completeExceptionally(
                  UnrecoverableWatchTransferException(watchResult?.toString())
               )
               false
            }

            TransmissionResult.FailedDifferentAppOpen -> {
               // Do not do anything. This coroutine will be cancelled any second now due to watchapp closing.
               false
            }
         }
      } while (retry)
   }

   private class Packet(
      val dictionary: PebbleDictionary,
      val priority: Int,
      val sentNofification: CompletableDeferred<Unit>,
   ) : Comparable<Packet> {
      override fun compareTo(other: Packet): Int {
         return -priority.compareTo(other.priority)
      }
   }
}

private val START_RETRY_DELAY = 100.milliseconds
