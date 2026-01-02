package com.matejdro.pebble.bluetooth.common

import com.matejdro.catapult.bluetooth.FakePebbleSender
import com.matejdro.catapult.bluetooth.FakePebbleSender.SentPacket
import com.matejdro.pebble.bluetooth.common.exceptions.UnrecoverableWatchTransferException
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.rebble.pebblekit2.common.model.PebbleDictionaryItem
import io.rebble.pebblekit2.common.model.TransmissionResult
import io.rebble.pebblekit2.common.model.WatchIdentifier
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import si.inova.kotlinova.core.test.time.virtualTimeProvider
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class PacketQueueTest {
   private val scope = TestScope()
   private val sender = FakePebbleSender(scope.virtualTimeProvider())

   private val packetQueue = PacketQueue(sender, WATCH_ID, WATCHAPP_UUID)

   @Test
   fun `Send data to the watch`() = scope.runTest {
      backgroundScope.launch {
         packetQueue.runQueue()
      }

      launch { packetQueue.sendPacket(mapOf(0u to PebbleDictionaryItem.UInt8(1u))) }
      runCurrent()

      sender.sentPackets.shouldContainExactly(
         SentPacketWithValue(1u)
      )
   }

   @Test
   fun `Send data to the watch in a priority order`() = scope.runTest {
      backgroundScope.launch {
         packetQueue.runQueue()
      }

      sender.pauseSending = true
      runCurrent()

      launch { packetQueue.sendPacket(mapOf(0u to PebbleDictionaryItem.UInt8(1u))) }
      runCurrent()

      launch { packetQueue.sendPacket(mapOf(0u to PebbleDictionaryItem.UInt8(2u)), priority = -1) }
      launch { packetQueue.sendPacket(mapOf(0u to PebbleDictionaryItem.UInt8(3u)), priority = 2) }
      launch { packetQueue.sendPacket(mapOf(0u to PebbleDictionaryItem.UInt8(4u)), priority = 1) }
      launch { packetQueue.sendPacket(mapOf(0u to PebbleDictionaryItem.UInt8(5u)), priority = -5) }
      sender.pauseSending = false
      runCurrent()

      sender.sentPackets.shouldContainExactly(
         SentPacketWithValue(1u), // First one is sent immediately because we don't need to wait for the ACK
         SentPacketWithValue(3u),
         SentPacketWithValue(4u),
         SentPacketWithValue(2u),
         SentPacketWithValue(5u),
      )
   }

   @Test
   fun `Suspend sendPacket until the packet is sent`() = scope.runTest {
      backgroundScope.launch {
         packetQueue.runQueue()
      }

      sender.pauseSending = true
      runCurrent()

      val sendingWait = launch { packetQueue.sendPacket(mapOf(0u to PebbleDictionaryItem.UInt8(1u))) }
      runCurrent()

      sendingWait.isCompleted shouldBe false

      sender.pauseSending = false
      runCurrent()
      sendingWait.isCompleted shouldBe true
   }

   @Test
   fun `Remove packet from queue if cancelled`() = scope.runTest {
      backgroundScope.launch {
         packetQueue.runQueue()
      }

      sender.pauseSending = true
      runCurrent()

      launch { packetQueue.sendPacket(mapOf(0u to PebbleDictionaryItem.UInt8(1u))) }
      runCurrent()

      val secondPacket = launch { packetQueue.sendPacket(mapOf(0u to PebbleDictionaryItem.UInt8(2u))) }
      runCurrent()

      secondPacket.cancel()
      runCurrent()

      sender.pauseSending = false
      runCurrent()

      sender.sentPackets.shouldContainExactly(
         SentPacketWithValue(1u),
      )
   }

   @Test
   fun `Throw unrecoverable exception when cannot connect`() = scope.runTest {
      sender.reportNoPebbleAppInstalled = true

      backgroundScope.launch {
         packetQueue.runQueue()
      }

      val e = assertThrows<UnrecoverableWatchTransferException> {
         packetQueue.sendPacket(mapOf(0u to PebbleDictionaryItem.UInt8(1u)))
      }

      e.message.shouldBe("No Pebble app is installed")
   }

   @Test
   fun `Throw unrecoverable exception when unrecoverable error happens`() = scope.runTest {
      sender.sendingResult = TransmissionResult.FailedNoPermissions

      backgroundScope.launch {
         packetQueue.runQueue()
      }

      val e = assertThrows<UnrecoverableWatchTransferException> {
         packetQueue.sendPacket(mapOf(0u to PebbleDictionaryItem.UInt8(1u)))
      }

      e.message.shouldBe("FailedNoPermissions")
   }

   @Test
   fun `Attempt exponential backoff when sending fails due to connection issues`() = scope.runTest {
      sender.sendingResult = TransmissionResult.FailedTimeout

      backgroundScope.launch {
         packetQueue.runQueue()
      }

      val packet = launch { packetQueue.sendPacket(mapOf(0u to PebbleDictionaryItem.UInt8(1u))) }
      delay(4.seconds)

      sender.sentPackets.shouldContainExactly(
         SentPacketWithValue(1u, time = 0.milliseconds),
         SentPacketWithValue(1u, time = 100.milliseconds), // Delay = 100 ms
         SentPacketWithValue(1u, time = 300.milliseconds), // Delay = 200 ms
         SentPacketWithValue(1u, time = 700.milliseconds), // Delay = 400 ms
         SentPacketWithValue(1u, time = 1_500.milliseconds), // Delay = 800 ms
         SentPacketWithValue(1u, time = 3_100.milliseconds), // Delay = 1_600 ms
      )

      packet.cancel()
   }
}

private fun SentPacketWithValue(value: UByte, time: Duration = Duration.ZERO): SentPacket = SentPacket(
   WATCHAPP_UUID,
   mapOf(0u to PebbleDictionaryItem.UInt8(value)),
   listOf(WATCH_ID),
   sentTime = time.inWholeMilliseconds,
)

private val WATCH_ID = WatchIdentifier("Watch")
private val WATCHAPP_UUID = UUID.fromString("882e39c3-de34-4dbc-a721-2eb4d1c30f29")
