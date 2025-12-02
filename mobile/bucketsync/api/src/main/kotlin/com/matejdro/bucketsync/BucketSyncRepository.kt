package com.matejdro.bucketsync

import com.matejdro.bucketsync.api.BucketUpdate

interface BucketSyncRepository {
   /**
    * Init the bucket sync with the current protocol version.
    *
    * When this method returns `false`, it means, that due to the protocol upgrade, all existing buckets were invalidated.
    * You need to call [updateBucket] again for all buckets to replenish the data.
    */
   suspend fun init(protocolVersion: Int): Boolean

   /**
    * Update bucket data for the bucket [id]. This will increment the internal version - this bucket will be included
    * in the next update.
    */
   suspend fun updateBucket(id: UByte, data: ByteArray)

   /**
    * Method will check whether a different version than the [currentVersion] is available. It will return a BucketUpdate
    * with the new version and all buckets that need to update or *null* if there is no such update
    */
   suspend fun awaitNextUpdate(currentVersion: UShort): BucketUpdate

   /**
    * Method will suspend until a different version than the [currentVersion] is available. Then it will return a BucketUpdate
    * with the new version and all buckets that need to update.
    *
    * This may be debounced (e.g. quick successive updates will only trigger a single update).
    */
   suspend fun checkForNextUpdate(currentVersion: UShort): BucketUpdate?

   /**
    * Delete data of the passed bucket from the watch
    */
   suspend fun deleteBucket(id: UByte)
}
