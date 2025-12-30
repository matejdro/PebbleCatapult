package com.matejdro.bucketsync.background

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkQuery
import androidx.work.await
import androidx.work.workDataOf
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.rebble.pebblekit2.PebbleKitProviderContract
import io.rebble.pebblekit2.client.PebbleAndroidAppPicker
import io.rebble.pebblekit2.common.model.WatchIdentifier
import kotlinx.coroutines.flow.first
import logcat.logcat
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Inject
@ContributesBinding(AppScope::class)
class WorkControllerImpl(
   private val workManager: WorkManager,
   private val pebbleAndroidAppPicker: PebbleAndroidAppPicker,
) : WorkController {
   // Background sync works in 3 stages:
   // 1. GetConnectedWatchesWorker - Gets connected watches, whenever the list of watches change.
   //    It schedules step 2 for all needed connected watches.
   // 2. GetForegroundAppWorker - It gets the current foreground app. If the app is not a watchface, it re-schedules itself.
   //    Otherwise, it schedules step 3 randomly after 10-30 minutes
   //    (long-ish timer to not hammer the watch with too many requests).
   // 3. StartSyncWorker - It opens the watch-app to start syncing

   override suspend fun cancelAllBackgroundWork() {
      workManager.cancelUniqueWork(WORK_NAME_CONNECTED_WATCHES).await()
      workManager.cancelAllWorkByTag(TAG_PER_WATCH_TAG).await()
   }

   override suspend fun scheduleBackgroundWork() {
      val packageName = pebbleAndroidAppPicker.getCurrentlySelectedApp()
      if (packageName == null) {
         logcat { "No Pebble Android App detected. Skipping connected watches schedule..." }
         return
      }

      workManager.enqueueUniqueWork(
         WORK_NAME_CONNECTED_WATCHES,
         ExistingWorkPolicy.REPLACE,
         OneTimeWorkRequestBuilder<GetConnectedWatchesWorker>()
            .build()
      ).await()
   }

   suspend fun scheduleConnectedWatchesWorkOnNextConnectedWatchChange() {
      val packageName = pebbleAndroidAppPicker.getCurrentlySelectedApp()
      if (packageName == null) {
         logcat { "No Pebble Android App detected. Skipping connected watches schedule..." }
         return
      }

      val watchesUri = PebbleKitProviderContract.ConnectedWatch.getContentUri(packageName)

      workManager.enqueueUniqueWork(
         WORK_NAME_CONNECTED_WATCHES,
         ExistingWorkPolicy.REPLACE,
         OneTimeWorkRequestBuilder<GetConnectedWatchesWorker>()
            .setConstraints(
               Constraints.Builder()
                  .addContentUriTrigger(watchesUri, false)
                  .setTriggerContentUpdateDelay(PEBBLE_STATE_TIMEOUT_DEBOUNCE_SECONDS, TimeUnit.SECONDS)
                  .build()
            )
            .build()
      )
         .await()
   }

   suspend fun scheduleForegroundAppWorkers(connectedWatches: List<WatchIdentifier>) {
      val existingWorks = workManager.getWorkInfosFlow(WorkQuery.fromTags(TAG_PER_WATCH_TAG)).first()

      val worksToCancel = existingWorks.filter { work ->
         val watchTag =
            work.tags.find { it.startsWith(TAG_WATCH_FOREGROUND_APP_PREFIX) }?.removePrefix(TAG_WATCH_FOREGROUND_APP_PREFIX)
               ?: work.tags.find { it.startsWith(TAG_WATCH_OPEN_PREFIX) }?.removePrefix(TAG_WATCH_OPEN_PREFIX)
               ?: return@filter true

         connectedWatches.none { it.value == watchTag }
      }

      for (work in worksToCancel) {
         logcat { "Work ${work.tags} ${work.state} cancelled" }
         workManager.cancelWorkById(work.id).await()
      }

      for (watch in connectedWatches) {
         val name = TAG_WATCH_FOREGROUND_APP_PREFIX + watch.value
         logcat { "Enqueueing $name" }
         workManager.enqueueUniqueWork(
            name,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<GetForegroundAppWorker>()
               .addTag(TAG_PER_WATCH_TAG)
               .addTag(name)
               .setInputData(workDataOf(GetForegroundAppWorker.DATA_KEY_WATCH to watch.value))
               .build()
         )
            .await()
      }
   }

   suspend fun scheduleForegroundAppWorkerOnNextChange(connectedWatch: WatchIdentifier) {
      val packageName = pebbleAndroidAppPicker.getCurrentlySelectedApp()
      if (packageName == null) {
         logcat { "No Pebble Android App detected. Skipping foreground app schedule..." }
         return
      }

      val foregroundAppUri = PebbleKitProviderContract.ActiveApp.getContentUri(packageName, connectedWatch)

      val name = TAG_WATCH_FOREGROUND_APP_PREFIX + connectedWatch.value
      logcat { "Enqueueing $name" }
      workManager.enqueueUniqueWork(
         name,
         ExistingWorkPolicy.REPLACE,
         OneTimeWorkRequestBuilder<GetForegroundAppWorker>()
            .setConstraints(
               Constraints.Builder()
                  .addContentUriTrigger(foregroundAppUri, false)
                  .setTriggerContentUpdateDelay(PEBBLE_STATE_TIMEOUT_DEBOUNCE_SECONDS, TimeUnit.SECONDS)
                  .build()
            )
            .addTag(TAG_PER_WATCH_TAG)
            .addTag(name)
            .setInputData(workDataOf(GetForegroundAppWorker.DATA_KEY_WATCH to connectedWatch.value))
            .build()
      )
         .await()
   }

   suspend fun scheduleOpenWatchappWorker(connectedWatch: WatchIdentifier) {
      val name = TAG_WATCH_OPEN_PREFIX + connectedWatch.value

      val duration = 10.minutes + Random.nextInt(20.minutes.inWholeSeconds.toInt()).seconds
      logcat { "Opening Watchapp on the watch $connectedWatch after $duration" }

      workManager.enqueueUniqueWork(
         name,
         ExistingWorkPolicy.KEEP,
         OneTimeWorkRequestBuilder<OpenWatchappWorker>()
            .setConstraints(
               Constraints.Builder()
                  .build()
            )
            .setInitialDelay(duration.inWholeSeconds, TimeUnit.SECONDS)
            .addTag(TAG_PER_WATCH_TAG)
            .addTag(name)
            .setInputData(workDataOf(OpenWatchappWorker.DATA_KEY_WATCH to connectedWatch.value))
            .build()
      ).await()
   }

   suspend fun cancelForegroundAppWorker(connectedWatch: WatchIdentifier) {
      workManager.cancelUniqueWork(TAG_WATCH_FOREGROUND_APP_PREFIX + connectedWatch.value).await()
   }

   suspend fun cancelWatchfaceOpen(connectedWatch: WatchIdentifier) {
      workManager.cancelUniqueWork(TAG_WATCH_OPEN_PREFIX + connectedWatch.value).await()
   }
}

private const val WORK_NAME_CONNECTED_WATCHES = "CONNECTED_WATCHES"
private const val TAG_PER_WATCH_TAG = "PER_WATCH"
private const val TAG_WATCH_FOREGROUND_APP_PREFIX = "WATCH_FOREGROUND_APP_"
private const val TAG_WATCH_OPEN_PREFIX = "WATCH_OPEN_"

private const val PEBBLE_STATE_TIMEOUT_DEBOUNCE_SECONDS = 10L

interface WorkController {
   suspend fun cancelAllBackgroundWork()

   suspend fun scheduleBackgroundWork()
}
