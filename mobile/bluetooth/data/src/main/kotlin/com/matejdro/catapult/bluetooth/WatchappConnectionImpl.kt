package com.matejdro.catapult.bluetooth

import com.matejdro.bucketsync.BucketSyncRepository
import com.matejdro.bucketsync.background.BackgroundSyncNotifier
import com.matejdro.catapult.actionlist.api.CatapultActionRepository
import com.matejdro.catapult.bluetooth.api.WATCHAPP_UUID
import com.matejdro.catapult.common.flow.firstData
import com.matejdro.catapult.tasker.TaskerTaskStarter
import com.matejdro.pebble.bluetooth.common.PacketQueue
import com.matejdro.pebble.bluetooth.common.WatchAppConnection
import com.matejdro.pebble.bluetooth.common.di.WatchappConnectionGraph
import com.matejdro.pebble.bluetooth.common.di.WatchappConnectionScope
import com.matejdro.pebble.bluetooth.common.util.requireUint
import com.matejdro.pebble.bluetooth.common.util.writeUByte
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.rebble.pebblekit2.client.PebbleSender
import io.rebble.pebblekit2.common.model.PebbleDictionary
import io.rebble.pebblekit2.common.model.PebbleDictionaryItem
import io.rebble.pebblekit2.common.model.ReceiveResult
import io.rebble.pebblekit2.common.model.WatchIdentifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import logcat.logcat
import okio.Buffer

@Inject
@ContributesBinding(WatchappConnectionScope::class)
@Suppress("MagicNumber") // Packet processing involves a lot of numbers, it would be less readable to make consts
class WatchappConnectionImpl(
   private val watch: WatchIdentifier,
   private val coroutineScope: CoroutineScope,
   private val bucketSyncRepository: BucketSyncRepository,
   private val actionRepository: CatapultActionRepository,
   private val taskerTaskStarter: TaskerTaskStarter,
   private val backgroundSyncNotifier: BackgroundSyncNotifier,
   private val watchappOpenController: WatchappOpenController,
   pebbleSender: PebbleSender,
) : WatchAppConnection {
   private val packetQueue = PacketQueue(pebbleSender, watch, WATCHAPP_UUID)
   private var watchBufferSize: Int = 0
   private var bucketSyncJob: Job? = null

   init {
      coroutineScope.launch {
         packetQueue.runQueue()
      }
   }

   override suspend fun onPacketReceived(data: PebbleDictionary): ReceiveResult {
      val id = (data.get(0u) as PebbleDictionaryItem.UInt32?)?.value
      logcat { "Received packet ${id ?: "null"} from $watch" }

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

      startBucketsyncLoop(watchVersion)

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

   private fun startBucketsyncLoop(initialWatchVersion: UShort) {
      bucketSyncJob?.cancel()
      bucketSyncJob = coroutineScope.launch {
         val bucketsyncBuffer = Buffer()
         val initialUpdate = bucketSyncRepository.checkForNextUpdate(initialWatchVersion)
         val watchVersion: UShort
         if (initialUpdate == null) {
            bucketsyncBuffer.writeUByte(SYNC_STATUS_UP_TO_DATE)

            packetQueue.sendPacket(
               mapOfNotNull(
                  0u to PebbleDictionaryItem.UInt8(1u),
                  1u to PebbleDictionaryItem.UInt16(PROTOCOL_VERSION),
                  2u to PebbleDictionaryItem.ByteArray(bucketsyncBuffer.readByteArray()),
                  (3u to PebbleDictionaryItem.UInt8(1u)).takeIf { watchappOpenController.isNextWatchappOpenForAutoSync() },
               ),
               PRIORITY_SYNC
            )
            watchVersion = initialWatchVersion
            backgroundSyncNotifier.notifyWatchFullySynced(watch.value)
            watchappOpenController.resetNextWatchappOpen()
         } else {
            logcat { "Sending bucketsync update: ${initialUpdate.toVersion} | ${initialUpdate.bucketsToUpdate.map { it.id }}" }
            val totalHelloSizeUntilBuckets = SIZE_OF_STATIC_PART_OF_HELLO_PACKET + 2 * initialUpdate.activeBuckets.size

            val extraPackets: List<PebbleDictionary> = createBucketsyncPackets(
               initialUpdate,
               bucketsyncBuffer,
               watchBufferSize - totalHelloSizeUntilBuckets,
               watchBufferSize
            )

            logcat { "Extra packets: ${extraPackets.size}" }

            packetQueue.sendPacket(
               mapOfNotNull(
                  0u to PebbleDictionaryItem.UInt8(1u),
                  1u to PebbleDictionaryItem.UInt16(PROTOCOL_VERSION),
                  2u to PebbleDictionaryItem.ByteArray(bucketsyncBuffer.readByteArray()),
                  (3u to PebbleDictionaryItem.UInt8(1u)).takeIf { watchappOpenController.isNextWatchappOpenForAutoSync() },
               ),
               PRIORITY_SYNC
            )
            watchappOpenController.resetNextWatchappOpen()

            for (packet in extraPackets) {
               packetQueue.sendPacket(
                  packet,
                  PRIORITY_SYNC
               )
            }

            watchVersion = initialUpdate.toVersion
            backgroundSyncNotifier.notifyWatchFullySynced(watch.value)
         }

         observeForFutureSyncs(watchVersion, bucketsyncBuffer)
      }
   }

   private suspend fun observeForFutureSyncs(
      initialWatchVersion: UShort,
      bucketsyncBuffer: Buffer,
   ) {
      var watchVersion = initialWatchVersion

      while (currentCoroutineContext().isActive) {
         val nextUpdate = bucketSyncRepository.awaitNextUpdate(watchVersion)
         logcat {
            "Phone updated while the watchapp is open: " +
               "${nextUpdate.toVersion} | ${nextUpdate.bucketsToUpdate.map { it.id }}"
         }

         val totalNewSyncSizeUntilBuckets = SIZE_OF_STATIC_PART_OF_NEW_UPDATE_PACKET + 2 * nextUpdate.activeBuckets.size

         val extraPackets: List<PebbleDictionary> = createBucketsyncPackets(
            nextUpdate,
            bucketsyncBuffer,
            watchBufferSize - totalNewSyncSizeUntilBuckets,
            watchBufferSize
         )

         logcat { "Extra packets: ${extraPackets.size}" }

         packetQueue.sendPacket(
            mapOf(
               0u to PebbleDictionaryItem.UInt8(2u),
               1u to PebbleDictionaryItem.ByteArray(bucketsyncBuffer.readByteArray()),
            ),
            PRIORITY_SYNC
         )

         for (packet in extraPackets) {
            packetQueue.sendPacket(
               packet,
               PRIORITY_SYNC
            )
         }

         watchVersion = nextUpdate.toVersion
         backgroundSyncNotifier.notifyWatchFullySynced(watch.value)
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

internal const val SYNC_STATUS_UP_TO_DATE: UByte = 2u
internal const val SYNC_STATUS_LAST_PACKET: UByte = 1u
internal const val SYNC_STATUS_MORE_PACKETS: UByte = 0u

private const val SIZE_OF_STATIC_PART_OF_HELLO_PACKET =
   1 +
      7 + 1 +
      7 + 1 +
      7 + 2 +
      7 +
      1 +
      16 +
      1

private const val SIZE_OF_STATIC_PART_OF_NEW_UPDATE_PACKET =
   1 + 7 + 2 + 1 + 1

private const val PRIORITY_SYNC = 0
