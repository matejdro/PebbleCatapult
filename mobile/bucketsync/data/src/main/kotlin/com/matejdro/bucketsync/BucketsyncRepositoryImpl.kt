package com.matejdro.bucketsync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import app.cash.sqldelight.coroutines.asFlow
import com.matejdro.bucketsync.api.Bucket
import com.matejdro.bucketsync.api.BucketUpdate
import com.matejdro.bucketsync.background.BackgroundSyncNotifier
import com.matejdro.bucketsync.sqldelight.generated.DbBucketQueries
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dispatch.core.withIO
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import logcat.logcat
import kotlin.time.Duration.Companion.milliseconds

@Inject
@ContributesBinding(AppScope::class)
class BucketsyncRepositoryImpl(
   private val queries: DbBucketQueries,
   private val preferences: DataStore<Preferences>,
   private val backgroundSyncNotifier: BackgroundSyncNotifier,
) : BucketSyncRepository {
   override suspend fun init(protocolVersion: Int): Boolean {
      val version = preferences.data.first()[lastVersionKey]
      logcat { "Bucket sync init from ${version ?: "START"} to $protocolVersion" }
      return if (version != protocolVersion) {
         preferences.edit { it[lastVersionKey] = protocolVersion }
         withIO {
            queries.deleteAll()
         }
         false
      } else {
         true
      }
   }

   override suspend fun updateBucket(id: UByte, data: ByteArray) = withIO<Unit> {
      queries.transaction {
         require(data.size <= MAX_BUCKET_SIZE) { "bucket size (${data.size}) must be at most 256 bytes" }
         logcat { "Update bucket $id (${data.size} bytes)" }
         queries.insert(id.toLong(), data)
         val updatedBucket = queries.getBucket(id.toLong()).executeAsOne()
         if (updatedBucket.version > UShort.MAX_VALUE.toLong()) {
            logcat { "Got over UShort_MAX, wrapping around" }
            queries.resetAllVersions()
         }
      }

      backgroundSyncNotifier.notifyDataChanged()
   }

   override suspend fun awaitNextUpdate(currentVersion: UShort): BucketUpdate = withIO {
      logcat { "Await next update from $currentVersion" }
      val versionFlow = queries.getLatestVersion().asFlow().map { it.executeAsOne().MAX?.toUShort() ?: 0u }
      val newVersion = versionFlow.debounce(BUCKET_UPDATE_DEBOUNCE).first { it != currentVersion }

      logcat { "Update $newVersion detected" }

      val requestVersion = if (newVersion > currentVersion) {
         currentVersion
      } else {
         // Do a full sync if watch's version is somehow higher than ours
         0u
      }

      createBucketUpdate(requestVersion, newVersion)
   }

   override suspend fun checkForNextUpdate(currentVersion: UShort): BucketUpdate? = withIO {
      logcat { "Check next update from $currentVersion" }
      val latestVersion = queries.getLatestVersion().executeAsOne().MAX?.toUShort() ?: 0u

      logcat { "Latest version is $latestVersion" }

      val requestVersion = if (currentVersion == latestVersion) {
         return@withIO null
      } else if (latestVersion > currentVersion) {
         currentVersion
      } else {
         // Do a full sync if watch's version is somehow higher than ours
         0u
      }

      createBucketUpdate(requestVersion, latestVersion)
   }

   private fun createBucketUpdate(requestVersion: UShort, newVersion: UShort): BucketUpdate {
      val bucketsToUpdate = queries.getUpdatedBuckets(requestVersion.toLong()).executeAsList()
      val activeBuckets = queries.getActiveBuckets().executeAsList().map { it.toUShort() }

      logcat { "Active buckets: $activeBuckets, bucketsToUpdate: ${bucketsToUpdate.map { it.id }}" }

      return BucketUpdate(
         newVersion,
         activeBuckets,
         bucketsToUpdate.map { Bucket(it.id.toUByte(), it.data_) }
      )
   }

   override suspend fun deleteBucket(id: UByte) = withIO<Unit> {
      logcat { "Delete bucket $id" }
      queries.insert(id.toLong(), null)
      backgroundSyncNotifier.notifyDataChanged()
   }
}

// Matching PERSIST_DATA_MAX_LENGTH of the watch SDK
private val BUCKET_UPDATE_DEBOUNCE = 100.milliseconds
private const val MAX_BUCKET_SIZE = 256

private val lastVersionKey = intPreferencesKey("bucketsync_last_version")
