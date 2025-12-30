package com.matejdro.bucketsync.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.matejdro.bucketsync.di.BucketSyncWorkerKey
import com.matejdro.bucketsync.sqldelight.generated.DbSyncStatusQueries
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.binding
import dispatch.core.withIO
import io.rebble.pebblekit2.client.PebbleInfoRetriever
import kotlinx.coroutines.flow.first
import logcat.logcat
import si.inova.kotlinova.core.time.TimeProvider
import kotlin.time.toJavaDuration

@AssistedInject
@IntoMap
class GetConnectedWatchesWorker(
   context: Context,
   private val workerController: WorkControllerImpl,
   private val pebbleInfoRetriever: PebbleInfoRetriever,
   private val statusDb: DbSyncStatusQueries,
   private val timeProvider: TimeProvider,
   @Assisted
   params: WorkerParameters,
) : CoroutineWorker(context, params) {
   override suspend fun doWork(): Result {
      val connectedWatches = pebbleInfoRetriever.getConnectedWatches().first()
      logcat { "Connected watches changed: ${connectedWatches.map { it.id to it.name }}" }

      val deadline = timeProvider.currentInstant() - WATCH_BACKGROUND_SYNC_TIMEOUT_TIME.toJavaDuration()
      val pendingSyncWatches = withIO { statusDb.getPendingSyncWatches(deadline.epochSecond).executeAsList() }

      val filteredWatches = connectedWatches.filter { pendingSyncWatches.contains(it.id.value) }
      logcat { "Connected watches with pending sync: ${filteredWatches.map { it.id to it.name }}" }

      workerController.scheduleForegroundAppWorkers(filteredWatches.map { it.id })
      workerController.scheduleConnectedWatchesWorkOnNextConnectedWatchChange()
      return Result.success()
   }

   @AssistedFactory
   @Inject
   @ContributesIntoMap(AppScope::class, binding<(WorkerParameters) -> ListenableWorker>())
   @BucketSyncWorkerKey(GetConnectedWatchesWorker::class)
   interface Factory : (WorkerParameters) -> ListenableWorker {
      fun create(params: WorkerParameters): GetConnectedWatchesWorker

      override fun invoke(params: WorkerParameters): ListenableWorker {
         return create(params)
      }
   }
}
