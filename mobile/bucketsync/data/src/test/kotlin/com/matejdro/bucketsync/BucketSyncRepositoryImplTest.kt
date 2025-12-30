package com.matejdro.bucketsync

import androidx.datastore.preferences.core.preferencesOf
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.matejdro.bucketsync.api.Bucket
import com.matejdro.bucketsync.api.BucketUpdate
import com.matejdro.bucketsync.background.FakeBackgroundSyncNotifier
import com.matejdro.bucketsync.sqldelight.generated.Database
import com.matejdro.bucketsync.sqldelight.generated.DbBucket
import com.matejdro.bucketsync.sqldelight.generated.DbBucketQueries
import com.matejdro.catapult.common.test.datastore.InMemoryDataStore
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import si.inova.kotlinova.core.test.TestScopeWithDispatcherProvider
import kotlin.time.Duration.Companion.seconds

class BucketSyncRepositoryImplTest {
   private val scope = TestScopeWithDispatcherProvider()
   private val db = createTestBucketQueries()
   private val notifier = FakeBackgroundSyncNotifier()
   private val repo = BucketsyncRepositoryImpl(db, InMemoryDataStore(preferencesOf()), notifier)

   @Test
   fun `Report all added buckets when updating from version 0`() = scope.runTest {
      repo.init(1)

      repo.updateBucket(1u, byteArrayOf(1))
      repo.updateBucket(2u, byteArrayOf(2))
      delay(1.seconds)

      val bucketsToUpdate = repo.awaitNextUpdate(0u)

      bucketsToUpdate shouldBe BucketUpdate(
         2u,
         listOf(1u, 2u),
         listOf(
            Bucket(1u, byteArrayOf(1)),
            Bucket(2u, byteArrayOf(2))
         )
      )
   }

   @Test
   fun `Report only the latest version of every bucket`() = scope.runTest {
      repo.init(1)

      repo.updateBucket(1u, byteArrayOf(1))
      repo.updateBucket(2u, byteArrayOf(2))
      repo.updateBucket(2u, byteArrayOf(3))
      delay(1.seconds)

      val bucketsToUpdate = repo.awaitNextUpdate(0u)

      bucketsToUpdate shouldBe BucketUpdate(
         3u,
         listOf(1u, 2u),
         listOf(
            Bucket(1u, byteArrayOf(1)),
            Bucket(2u, byteArrayOf(3))
         )
      )
   }

   @Test
   fun `Await until the new update is ready`() = scope.runTest {
      repo.init(1)

      repo.updateBucket(1u, byteArrayOf(1))
      repo.updateBucket(2u, byteArrayOf(2))
      delay(1.seconds)

      val bucketsToUpdate = async { repo.awaitNextUpdate(2u) }
      runCurrent()
      bucketsToUpdate.isCompleted shouldBe false

      repo.updateBucket(2u, byteArrayOf(3))
      delay(1.seconds)

      bucketsToUpdate.getCompleted() shouldBe BucketUpdate(
         3u,
         listOf(1u, 2u),
         listOf(
            Bucket(2u, byteArrayOf(3))
         )
      )
   }

   @Test
   fun `Await until the first bucket update is ready`() = scope.runTest {
      repo.init(1)

      val bucketsToUpdate = async { repo.awaitNextUpdate(0u) }

      runCurrent()
      bucketsToUpdate.isCompleted shouldBe false

      repo.updateBucket(2u, byteArrayOf(3))
      delay(1.seconds)

      bucketsToUpdate.getCompleted() shouldBe BucketUpdate(
         1u,
         listOf(2u),
         listOf(
            Bucket(2u, byteArrayOf(3))
         )
      )
   }

   @Test
   fun `Remove deleted buckets from the active bucket list`() = scope.runTest {
      repo.init(1)

      repo.updateBucket(1u, byteArrayOf(1))
      repo.updateBucket(2u, byteArrayOf(2))
      delay(1.seconds)

      repo.deleteBucket(2u)
      runCurrent()

      val bucketsToUpdate = repo.awaitNextUpdate(2u)

      bucketsToUpdate shouldBe BucketUpdate(
         3u,
         listOf(1u),
         emptyList(),
      )
   }

   @Test
   fun `Always return false from initial init call`() = scope.runTest {
      repo.init(1) shouldBe false
   }

   @Test
   fun `Do not return false from subsequent init calls with the same version`() = scope.runTest {
      repo.init(1) shouldBe false
      repo.init(1) shouldBe true
   }

   @Test
   fun `Do not wipe the database when calling init with the same protocol number`() = scope.runTest {
      repo.init(1)

      repo.updateBucket(1u, byteArrayOf(1))
      repo.updateBucket(2u, byteArrayOf(2))
      delay(1.seconds)

      repo.init(1)
      runCurrent()

      val bucketsToUpdate = async { repo.awaitNextUpdate(0u) }
      delay(1.seconds)
      bucketsToUpdate.isCompleted shouldBe true
   }

   @Test
   fun `Wipe the database when calling init with the a different protocol number`() = scope.runTest {
      repo.init(1)

      repo.updateBucket(1u, byteArrayOf(1))
      repo.updateBucket(2u, byteArrayOf(2))
      delay(1.seconds)

      repo.init(2)
      runCurrent()

      val bucketsToUpdate = async { repo.awaitNextUpdate(0u) }
      runCurrent()
      bucketsToUpdate.isCompleted shouldBe false

      bucketsToUpdate.cancel()
   }

   @Test
   fun `Debounce all updates made in a short span into a single update`() = scope.runTest {
      repo.init(1)

      val bucketsToUpdate = async { repo.awaitNextUpdate(0u) }

      repo.updateBucket(1u, byteArrayOf(1))
      repo.updateBucket(2u, byteArrayOf(2))
      runCurrent()
      repo.updateBucket(2u, byteArrayOf(3))
      delay(1.seconds)

      bucketsToUpdate.getCompleted() shouldBe BucketUpdate(
         3u,
         listOf(1u, 2u),
         listOf(
            Bucket(1u, byteArrayOf(1)),
            Bucket(2u, byteArrayOf(3))
         )
      )
   }

   @Test
   fun `Disallow adding buckets larger than 256 bytes`() = scope.runTest {
      repo.init(1)

      assertThrows<IllegalArgumentException> {
         repo.updateBucket(1u, ByteArray(300))
      }
   }

   @Test
   fun `Do not trigger an update when the bucket does not change`() = scope.runTest {
      repo.init(1)

      repo.updateBucket(1u, byteArrayOf(1))
      repo.updateBucket(2u, byteArrayOf(2))
      delay(1.seconds)

      repo.updateBucket(2u, byteArrayOf(2))
      delay(1.seconds)

      val bucketsToUpdate = async { repo.awaitNextUpdate(2u) }
      delay(1.seconds)

      bucketsToUpdate.isCompleted shouldBe false
      bucketsToUpdate.cancel()
   }

   @Test
   fun `Resync all data if requested version is higher than the database version`() = scope.runTest {
      repo.init(1)

      repo.updateBucket(1u, byteArrayOf(1))
      repo.updateBucket(2u, byteArrayOf(2))
      delay(1.seconds)

      repo.updateBucket(2u, byteArrayOf(3))
      delay(1.seconds)

      repo.awaitNextUpdate(6u) shouldBe BucketUpdate(
         3u,
         listOf(1u, 2u),
         listOf(
            Bucket(1u, byteArrayOf(1)),
            Bucket(2u, byteArrayOf(3))
         )
      )
   }

   @Test
   fun `Report all added buckets when peeking from version 0`() = scope.runTest {
      repo.init(1)

      repo.updateBucket(1u, byteArrayOf(1))
      repo.updateBucket(2u, byteArrayOf(2))
      delay(1.seconds)

      val bucketsToUpdate = repo.checkForNextUpdate(0u)

      bucketsToUpdate shouldBe BucketUpdate(
         2u,
         listOf(1u, 2u),
         listOf(
            Bucket(1u, byteArrayOf(1)),
            Bucket(2u, byteArrayOf(2))
         )
      )
   }

   @Test
   fun `Return null if there is no update ready during update peek`() = scope.runTest {
      repo.init(1)

      repo.updateBucket(1u, byteArrayOf(1))
      repo.updateBucket(2u, byteArrayOf(2))
      delay(1.seconds)

      repo.checkForNextUpdate(2u) shouldBe null
   }

   @Test
   fun `Resync all data if requested version is higher than the database version during check`() = scope.runTest {
      repo.init(1)

      repo.updateBucket(1u, byteArrayOf(1))
      repo.updateBucket(2u, byteArrayOf(2))
      delay(1.seconds)

      repo.updateBucket(2u, byteArrayOf(3))
      delay(1.seconds)

      repo.checkForNextUpdate(6u) shouldBe BucketUpdate(
         3u,
         listOf(1u, 2u),
         listOf(
            Bucket(1u, byteArrayOf(1)),
            Bucket(2u, byteArrayOf(3))
         )
      )
   }

   @Test
   fun `When version passes 65535 (ushort max), it should wrap around back to one`() = scope.runTest {
      db.insertRaw(
         DbBucket(1, byteArrayOf(1), 65535)
      )

      repo.init(1)

      repo.updateBucket(1u, byteArrayOf(1))
      delay(1.seconds)

      val bucketsToUpdate = repo.checkForNextUpdate(65535u)

      bucketsToUpdate shouldBe BucketUpdate(
         1u,
         listOf(1u),
         listOf(
            Bucket(1u, byteArrayOf(1)),
         )
      )
   }

   @Test
   fun `Report data changed when buckets are updated`() = scope.runTest {
      repo.init(1)

      repo.updateBucket(1u, byteArrayOf(1))
      repo.updateBucket(2u, byteArrayOf(2))
      delay(1.seconds)

      notifier.dataChangeNotified shouldBe true
   }

   @Test
   fun `Report data changed when bucket is deleted`() = scope.runTest {
      repo.init(1)

      repo.updateBucket(1u, byteArrayOf(1))
      repo.updateBucket(2u, byteArrayOf(2))
      delay(1.seconds)

      notifier.dataChangeNotified = false

      repo.deleteBucket(2u)
      runCurrent()

      notifier.dataChangeNotified shouldBe true
   }
}

private fun createTestBucketQueries(
   driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.Companion.IN_MEMORY).apply {
      Database.Companion.Schema.create(
         this
      )
   },
): DbBucketQueries {
   return Database.Companion(driver).dbBucketQueries
}
