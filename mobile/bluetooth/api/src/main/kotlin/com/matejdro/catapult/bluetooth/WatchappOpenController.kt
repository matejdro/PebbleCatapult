package com.matejdro.catapult.bluetooth

interface WatchappOpenController {
   fun isNextWatchappOpenForAutoSync(): Boolean
   fun setNextWatchappOpenForAutoSync()
   fun resetNextWatchappOpen()
}
