package com.matejdro.bucketsync.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.matejdro.bucketsync.di.BucketSyncWorkerKey
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.binding
import io.rebble.pebblekit2.client.PebbleInfoRetriever
import io.rebble.pebblekit2.common.model.WatchIdentifier
import io.rebble.pebblekit2.model.Watchapp
import kotlinx.coroutines.flow.first
import logcat.logcat
import si.inova.kotlinova.core.reporting.ErrorReporter

@AssistedInject
@IntoMap
class GetForegroundAppWorker(
   context: Context,
   private val workerController: WorkControllerImpl,
   private val pebbleinfoRetriever: PebbleInfoRetriever,
   private val errorReporter: ErrorReporter,
   @Assisted
   private val params: WorkerParameters,
) : CoroutineWorker(context, params) {
   override suspend fun doWork(): Result {
      val watchId = params.inputData.getString(DATA_KEY_WATCH)?.let { WatchIdentifier(it) }
      if (watchId == null) {
         errorReporter.report(Exception("Got missing watch ID"))
         return Result.failure()
      }

      val foregroundApp = pebbleinfoRetriever.getActiveApp(watchId).first()
      logcat { "Foreground app for $watchId: ${foregroundApp ?: "null"}" }

      if (foregroundApp?.isWatchface == Watchapp.Type.WATCHFACE) {
         logcat { "It's watchface! Scheduling open..." }
         workerController.scheduleOpenWatchappWorker(watchId)
      } else {
         logcat { "It's not a watchface! Cancelling open..." }
         workerController.cancelWatchfaceOpen(watchId)
      }
      workerController.scheduleForegroundAppWorkerOnNextChange(watchId)
      return Result.success()
   }

   @AssistedFactory
   @Inject
   @ContributesIntoMap(AppScope::class, binding<(WorkerParameters) -> ListenableWorker>())
   @BucketSyncWorkerKey(GetForegroundAppWorker::class)
   interface Factory : (WorkerParameters) -> ListenableWorker {
      fun create(params: WorkerParameters): GetForegroundAppWorker

      override fun invoke(params: WorkerParameters): ListenableWorker {
         return create(params)
      }
   }

   companion object {
      const val DATA_KEY_WATCH = "Watch"
   }
}
