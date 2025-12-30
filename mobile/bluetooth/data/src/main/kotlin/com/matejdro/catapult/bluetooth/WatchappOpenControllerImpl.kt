package com.matejdro.catapult.bluetooth

import com.matejdro.bucketsync.BucketSyncAutoSyncNotifier
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding<WatchappOpenController>())
@ContributesBinding(AppScope::class, binding<BucketSyncAutoSyncNotifier>())
class WatchappOpenControllerImpl : WatchappOpenController, BucketSyncAutoSyncNotifier {
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

   override fun notifyAboutToStartAutoSync() {
      setNextWatchappOpenForAutoSync()
   }
}
