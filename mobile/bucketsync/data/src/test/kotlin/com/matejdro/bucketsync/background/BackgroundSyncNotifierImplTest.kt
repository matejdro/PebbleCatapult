package com.matejdro.bucketsync.background

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.matejdro.bucketsync.sqldelight.generated.Database
import com.matejdro.bucketsync.sqldelight.generated.DbSyncStatusQueries
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import si.inova.kotlinova.core.test.TestScopeWithDispatcherProvider
import si.inova.kotlinova.core.test.time.virtualTimeProvider
import kotlin.time.Duration.Companion.days

class BackgroundSyncNotifierImplTest {
   private val scope = TestScopeWithDispatcherProvider()
   private val workController = FakeWorkController()
   private val notifier = BackgroundSyncNotifierImpl(
      workController,
      createTestSyncStatusQueries(),
      scope.virtualTimeProvider()
   )

   @Test
   fun `Stop syncing when all watches are fully synced`() = scope.runTest {
      notifier.notifyWatchFullySynced("watch1")

      workController.cancelledBackgroundWork shouldBe true
   }

   @Test
   fun `Start syncing when data changes`() = scope.runTest {
      notifier.notifyWatchFullySynced("watch1")
      notifier.notifyDataChanged()

      workController.startedBackgroundWork shouldBe true
   }

   @Test
   fun `Do not start syncing when data changes without any known watches`() = scope.runTest {
      notifier.notifyDataChanged()

      workController.startedBackgroundWork shouldBe false
   }

   @Test
   fun `Do not start syncing when data changes without any active watches`() = scope.runTest {
      notifier.notifyWatchFullySynced("watch1")
      delay(40.days)

      notifier.notifyDataChanged()

      workController.startedBackgroundWork shouldBe false
   }

   @Test
   fun `Do not start syncing on the startup when there is no changed data`() = scope.runTest {
      notifier.notifyAppStarted()

      workController.startedBackgroundWork shouldBe false
   }

   @Test
   fun `Start syncing on the startup when there is changed data`() = scope.runTest {
      notifier.notifyWatchFullySynced("watch1")
      notifier.notifyDataChanged()
      workController.startedBackgroundWork = false

      notifier.notifyAppStarted()

      workController.startedBackgroundWork shouldBe true
   }

   @Test
   fun `Cancel sync after watch is fully synced`() = scope.runTest {
      notifier.notifyWatchFullySynced("watch1")
      notifier.notifyDataChanged()
      workController.cancelledBackgroundWork = false

      notifier.notifyWatchFullySynced("watch1")
      workController.cancelledBackgroundWork shouldBe true
   }
}

private fun createTestSyncStatusQueries(
   driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).apply {
      Database.Schema.create(
         this
      )
   },
): DbSyncStatusQueries {
   return Database(driver).dbSyncStatusQueries
}
