package com.matejdro.bucketsync.background

import com.matejdro.bucketsync.sqldelight.generated.DbSyncStatus
import com.matejdro.bucketsync.sqldelight.generated.DbSyncStatusQueries
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dispatch.core.withIO
import logcat.logcat
import si.inova.kotlinova.core.time.TimeProvider
import kotlin.time.Duration.Companion.days
import kotlin.time.toJavaDuration

@Inject
@ContributesBinding(AppScope::class)
class BackgroundSyncNotifierImpl(
   private val workController: WorkController,
   private val dbSyncStatus: DbSyncStatusQueries,
   private val timeProvider: TimeProvider,
) : BackgroundSyncNotifier {
   override suspend fun notifyDataChanged() = withIO {
      dbSyncStatus.invalidateAllWatches()
      scheduleIfNeeded()
   }

   override suspend fun notifyWatchFullySynced(watch: String) = withIO {
      dbSyncStatus.insert(DbSyncStatus(watch, 1L, timeProvider.currentInstant().epochSecond))
      scheduleIfNeeded()
   }

   override suspend fun notifyAppStarted() {
      scheduleIfNeeded()
   }

   private suspend fun scheduleIfNeeded() {
      val deadline = timeProvider.currentInstant() - WATCH_BACKGROUND_SYNC_TIMEOUT_TIME.toJavaDuration()

      val anyPendingWatches = dbSyncStatus.isAnyWatchPendingSync(deadline.epochSecond).executeAsOne() > 0
      if (anyPendingWatches) {
         logcat { "Pending watches, scheduling background work" }
         workController.scheduleBackgroundWork()
      } else {
         logcat { "No pending watches, cancelling background work" }
         workController.cancelAllBackgroundWork()
      }
   }
}

/**
 * We have no way of knowing whether the watch is active or not. User might have connected a watch once and then stopped using it.
 * To conserve resources, we ignore watches that have not connected in the last 30 days.
 */
internal val WATCH_BACKGROUND_SYNC_TIMEOUT_TIME = 30.days
