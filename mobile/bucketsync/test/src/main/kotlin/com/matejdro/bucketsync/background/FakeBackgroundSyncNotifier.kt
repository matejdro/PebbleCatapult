package com.matejdro.bucketsync.background

class FakeBackgroundSyncNotifier : BackgroundSyncNotifier {
   var dataChangeNotified: Boolean = false
   val watchesFullySynced: MutableList<String> = ArrayList()

   override suspend fun notifyDataChanged() {
      dataChangeNotified = true
   }

   override suspend fun notifyWatchFullySynced(watch: String) {
      watchesFullySynced.add(watch)
   }

   override suspend fun notifyAppStarted() {
      throw UnsupportedOperationException("Not faked in tests")
   }
}
