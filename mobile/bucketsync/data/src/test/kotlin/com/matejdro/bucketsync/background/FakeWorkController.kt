package com.matejdro.bucketsync.background

class FakeWorkController : WorkController {
   var cancelledBackgroundWork: Boolean = false
   var startedBackgroundWork: Boolean = false

   override suspend fun cancelAllBackgroundWork() {
      cancelledBackgroundWork = true
   }

   override suspend fun scheduleBackgroundWork() {
      startedBackgroundWork = true
   }
}
