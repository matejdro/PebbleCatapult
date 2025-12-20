package com.matejdro.catapult.bluetooth

import com.matejdro.bucketsync.FakeBucketSyncRepository
import com.matejdro.bucketsync.api.Bucket
import com.matejdro.bucketsync.api.BucketUpdate
import com.matejdro.catapult.actionlist.api.CatapultAction
import com.matejdro.catapult.actionlist.api.CatapultDirectory
import com.matejdro.catapult.actionlist.test.FakeCatapultActionRepository
import com.matejdro.catapult.actionlist.test.FakeDirectoryListRepository
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import si.inova.kotlinova.core.test.TestScopeWithDispatcherProvider
import kotlin.time.Duration.Companion.seconds

class WatchSyncerImplTest {
   val bucketSyncRepository = FakeBucketSyncRepository(PROTOCOL_VERSION.toInt())
   val actionRepository = FakeCatapultActionRepository()
   val directoryRepository = FakeDirectoryListRepository()

   private val scope = TestScopeWithDispatcherProvider()

   val watchSyncer = WatchSyncerImpl(
      bucketSyncRepository,
      lazy { actionRepository },
      lazy { directoryRepository }
   )

   @Test
   fun `Sync directory to the watch`() = scope.runTest {
      watchSyncer.init()

      directoryRepository.insert(CatapultDirectory(1, "Directory 1"))

      actionRepository.insert(CatapultAction("Action A", directoryId = 1, id = 10))
      actionRepository.insert(CatapultAction("Action B", directoryId = 1, id = 11, targetDirectoryId = 2))
      actionRepository.insert(CatapultAction("Action C", directoryId = 1, id = 11, targetDirectoryId = 2, enabled = false))

      watchSyncer.syncDirectory(1)
      delay(1.seconds)

      bucketSyncRepository.awaitNextUpdate(0u) shouldBe BucketUpdate(
         1u,
         listOf(1u),
         listOf(
            Bucket(
               1u,
               byteArrayOf(
                  // Number of items
                  2,

                  // Action A
                  // ID
                  0, 10,
                  // Target directory (not applicable, so zero)
                  0,
                  // Flags (Unused for now)
                  0,
                  // Title
                  0x41, 0x63, 0x74, 0x69, 0x6f, 0x6e, 0x20, 0x41, 0,

                  // Action B
                  // ID
                  0, 11,
                  // Target directory
                  2,
                  // Flags (Unused for now)
                  0,
                  // Title
                  0x41, 0x63, 0x74, 0x69, 0x6f, 0x6e, 0x20, 0x42, 0
               )
            )
         )
      )
   }

   @Test
   fun `Delete a directory from the watch`() = scope.runTest {
      watchSyncer.init()

      directoryRepository.insert(CatapultDirectory(1, "Directory 1"))

      actionRepository.insert(CatapultAction("Action A", directoryId = 1, id = 10))
      actionRepository.insert(CatapultAction("Action B", directoryId = 1, id = 11, targetDirectoryId = 2))

      watchSyncer.syncDirectory(1)
      delay(1.seconds)

      watchSyncer.deleteDirectory(1)
      delay(1.seconds)

      bucketSyncRepository.awaitNextUpdate(0u) shouldBe BucketUpdate(
         2u,
         emptyList(),
         emptyList(),
      )
   }

   @Test
   fun `Reload all data when protocol changes`() = scope.runTest {
      val bucketSyncRepositoryWithOldVersion = FakeBucketSyncRepository(0)

      val watchSyncer = WatchSyncerImpl(
         bucketSyncRepositoryWithOldVersion,
         lazy { actionRepository },
         lazy { directoryRepository }
      )

      directoryRepository.insert(CatapultDirectory(1, "Directory 1"))
      directoryRepository.insert(CatapultDirectory(2, "Directory 2"))

      watchSyncer.init()

      val update = async { bucketSyncRepositoryWithOldVersion.awaitNextUpdate(0u) }
      delay(1.seconds)

      update.getCompleted().activeBuckets.shouldContainExactly(1u, 2u)
   }

   @Test
   fun `Do not reload all data when protocol changes`() = scope.runTest {
      directoryRepository.insert(CatapultDirectory(1, "Directory 1"))
      directoryRepository.insert(CatapultDirectory(2, "Directory 2"))

      watchSyncer.init()

      val update = async { bucketSyncRepository.awaitNextUpdate(0u) }
      delay(1.seconds)
      update.isCompleted shouldBe false
      update.cancel()
   }

   @Test
   fun `Only sync first 13 actions to the watch`() = scope.runTest {
      watchSyncer.init()

      directoryRepository.insert(CatapultDirectory(1, "Directory 1"))
      repeat(15) {
         val id = it + 1
         actionRepository.insert(CatapultAction("Action $it", directoryId = 1, id = id))
      }

      watchSyncer.syncDirectory(1)
      delay(1.seconds)

      bucketSyncRepository.awaitNextUpdate(0u).bucketsToUpdate.first().data.first() shouldBe 13
   }
}
