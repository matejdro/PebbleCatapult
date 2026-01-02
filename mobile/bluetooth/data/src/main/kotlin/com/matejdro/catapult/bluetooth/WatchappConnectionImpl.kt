package com.matejdro.catapult.bluetooth

import com.matejdro.bucketsync.BucketSyncWatchLoop
import com.matejdro.catapult.actionlist.api.CatapultActionRepository
import com.matejdro.catapult.common.flow.firstData
import com.matejdro.catapult.tasker.TaskerTaskStarter
import com.matejdro.pebble.bluetooth.common.PacketQueue
import com.matejdro.pebble.bluetooth.common.WatchAppConnection
import com.matejdro.pebble.bluetooth.common.di.WatchappConnectionGraph
import com.matejdro.pebble.bluetooth.common.di.WatchappConnectionScope
import com.matejdro.pebble.bluetooth.common.util.requireUint
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.rebble.pebblekit2.common.model.PebbleDictionary
import io.rebble.pebblekit2.common.model.PebbleDictionaryItem
import io.rebble.pebblekit2.common.model.PebbleDictionaryItem.UInt16
import io.rebble.pebblekit2.common.model.PebbleDictionaryItem.UInt8
import io.rebble.pebblekit2.common.model.ReceiveResult
import io.rebble.pebblekit2.common.model.WatchIdentifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import logcat.logcat

@Inject
@ContributesBinding(WatchappConnectionScope::class)
@Suppress("MagicNumber") // Packet processing involves a lot of numbers, it would be less readable to make consts
class WatchappConnectionImpl(
   coroutineScope: CoroutineScope,
   private val actionRepository: CatapultActionRepository,
   private val taskerTaskStarter: TaskerTaskStarter,
   private val watchappOpenController: WatchappOpenController,
   private val packetQueue: PacketQueue,
   private val bucketSyncWatchLoop: BucketSyncWatchLoop,
) : WatchAppConnection {
   private var watchBufferSize: Int = 0

   init {
      coroutineScope.launch {
         packetQueue.runQueue()
      }
   }

   override suspend fun onPacketReceived(data: PebbleDictionary): ReceiveResult {
      val id = (data.get(0u) as PebbleDictionaryItem.UInt32?)?.value
      logcat { "Received packet ${id ?: "null"}" }

      return when (id) {
         0u -> {
            processWatchWelcomePacket(data)
         }

         4u -> {
            processStartTaskPacket(data)
         }

         else -> {
            logcat { "Unknown packet ID. Nacking..." }
            ReceiveResult.Nack
         }
      }
   }

   private suspend fun processWatchWelcomePacket(data: PebbleDictionary): ReceiveResult {
      val watchProtocolVersion = data.requireUint(1u)
      if (watchProtocolVersion != PROTOCOL_VERSION.toUInt()) {
         logcat { "Mismatch protocol version $watchProtocolVersion" }
         packetQueue.sendPacket(
            mapOf(
               0u to PebbleDictionaryItem.UInt8(1u),
               1u to PebbleDictionaryItem.UInt16(PROTOCOL_VERSION)
            )
         )
         return ReceiveResult.Ack
      }

      val watchVersion = data.requireUint(2u).toUShort()
      watchBufferSize = data.requireUint(3u).toInt()
      logcat { "Watch data: version=$watchVersion, buffer size=$watchBufferSize" }

      bucketSyncWatchLoop.sendFirstPacketAndStartLoop(
         mapOfNotNull(
            0u to UInt8(1u),
            1u to UInt16(PROTOCOL_VERSION),
            (3u to UInt8(1u)).takeIf { watchappOpenController.isNextWatchappOpenForAutoSync() },
         ),
         watchVersion,
         watchBufferSize
      )

      return ReceiveResult.Ack
   }

   private suspend fun processStartTaskPacket(data: PebbleDictionary): ReceiveResult {
      val actionId = data.requireUint(1u)
      val action = actionRepository.getById(actionId.toInt()).firstData()
      if (action == null) {
         logcat { "Unknown action. Nacking..." }
         return ReceiveResult.Nack
      }

      val taskerTask = action.taskerTaskName
      if (taskerTask == null) {
         logcat { "Target action has no task. Nacking..." }
         return ReceiveResult.Nack
      }

      logcat { "Starting task $taskerTask" }

      val success = taskerTaskStarter.startTask(taskerTask)

      return if (success) {
         logcat { "Task successfully started" }
         ReceiveResult.Ack
      } else {
         logcat { "Tasker task launch failed" }
         ReceiveResult.Nack
      }
   }

   @Inject
   @ContributesBinding(AppScope::class)
   class Factory(
      private val subgraphFactory: WatchappConnectionGraph.Factory,
   ) : WatchAppConnection.Factory {
      override fun create(watch: WatchIdentifier, scope: CoroutineScope): WatchAppConnection {
         return subgraphFactory.create(scope, watch).createWatchappConnection()
      }
   }
}

private fun <K, V> mapOfNotNull(vararg pairs: Pair<K, V>?): Map<K, V> =
   pairs.filterNotNull().toMap()
