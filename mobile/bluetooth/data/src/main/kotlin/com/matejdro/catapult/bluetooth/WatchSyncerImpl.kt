package com.matejdro.catapult.bluetooth

import com.matejdro.bucketsync.BucketSyncRepository
import com.matejdro.catapult.actionlist.api.CatapultActionRepository
import com.matejdro.catapult.actionlist.api.DirectoryListRepository
import com.matejdro.catapult.bluetooth.util.writeUByte
import com.matejdro.catapult.bluetooth.util.writeUShort
import com.matejdro.catapult.common.flow.firstData
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dispatch.core.withDefault
import okio.Buffer
import si.inova.kotlinova.core.logging.logcat

@Inject
@ContributesBinding(AppScope::class)
class WatchSyncerImpl(
   private val bucketSyncRepository: BucketSyncRepository,
   private val actionRepository: Lazy<CatapultActionRepository>,
   private val directoryRepository: Lazy<DirectoryListRepository>,
) : WatchSyncer {
   override suspend fun init() {
      val reloadAllData = !bucketSyncRepository.init(PROTOCOL_VERSION.toInt())
      if (reloadAllData) {
         logcat { "Got different protocol version, resetting all data" }
         val allDirectories = directoryRepository.value.getAll().firstData()
         for (directory in allDirectories) {
            syncDirectory(directory.id)
         }
      }
   }

   override suspend fun syncDirectory(id: Int) = withDefault {
      val items = actionRepository.value.getAll(id, limit = MAX_ACTIONS_TO_SYNC, onlyEnabled = true).firstData()
      logcat { "Syncing directory $id, ${items.size} items" }

      val buffer = Buffer()

      buffer.writeUByte(items.size.toUByte())
      for (item in items) {
         buffer.writeUShort(item.id.toUShort())
         buffer.writeUByte(item.targetDirectoryId?.toUByte() ?: 0u)
         buffer.writeUByte(0u) // Flags, unused for now
         buffer.writeUtf8(item.title)
         buffer.writeUByte(0u) // Null terminator
      }
      val data = buffer.readByteArray()
      logcat { "Size: ${data.size} bytes" }

      bucketSyncRepository.updateBucket(id.toUByte(), data)
   }

   override suspend fun deleteDirectory(id: Int) {
      logcat { "Deleting directory $id" }
      bucketSyncRepository.deleteBucket(id.toUByte())
   }
}

private const val MAX_ACTIONS_TO_SYNC = 13
