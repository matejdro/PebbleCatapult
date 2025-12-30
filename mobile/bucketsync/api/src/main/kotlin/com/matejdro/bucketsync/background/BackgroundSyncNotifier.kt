package com.matejdro.bucketsync.background

interface BackgroundSyncNotifier {
   suspend fun notifyDataChanged()
   suspend fun notifyWatchFullySynced(watch: String)
   suspend fun notifyAppStarted()
}
