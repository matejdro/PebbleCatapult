package com.matejdro.catapult.bluetooth

class FakeWatchSyncer : WatchSyncer {
   val syncedDirectories = ArrayList<Int>()
   val deletedDirectories = ArrayList<Int>()

   override suspend fun init() {
   }

   override suspend fun syncDirectory(id: Int) {
      syncedDirectories.add(id)
   }

   override suspend fun deleteDirectory(id: Int) {
      deletedDirectories.add(id)
   }
}
