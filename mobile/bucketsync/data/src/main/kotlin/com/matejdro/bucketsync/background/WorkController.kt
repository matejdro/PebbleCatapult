package com.matejdro.bucketsync.background

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@Inject
@ContributesBinding(AppScope::class)
class WorkControllerImpl : WorkController {
   override suspend fun cancelAllBackgroundWork() {
   }

   override suspend fun scheduleBackgroundWork() {
   }
}

interface WorkController {
   suspend fun cancelAllBackgroundWork()

   suspend fun scheduleBackgroundWork()
}
