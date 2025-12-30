package com.matejdro.catapult.bluetooth

class FakeWatchappOpenController : WatchappOpenController {
   private var nextWatchappOpenForAutoSync: Boolean = false

   override fun isNextWatchappOpenForAutoSync(): Boolean {
      return nextWatchappOpenForAutoSync
   }

   override fun setNextWatchappOpenForAutoSync() {
      nextWatchappOpenForAutoSync = true
   }

   override fun resetNextWatchappOpen() {
      nextWatchappOpenForAutoSync = false
   }
}
