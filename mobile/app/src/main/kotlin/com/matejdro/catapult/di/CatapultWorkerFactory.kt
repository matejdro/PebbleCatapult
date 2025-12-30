package com.matejdro.catapult.di

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import dev.zacsweers.metro.Inject
import kotlin.reflect.KClass

@Inject
class CatapultWorkerFactory(
   private val factories: Map<KClass<out ListenableWorker>, (WorkerParameters) -> ListenableWorker>,
) : WorkerFactory() {
   override fun createWorker(
      appContext: Context,
      workerClassName: String,
      workerParameters: WorkerParameters,
   ): ListenableWorker? {
      val factory = factories[Class.forName(workerClassName).kotlin] ?: return null
      return factory(workerParameters)
   }
}
