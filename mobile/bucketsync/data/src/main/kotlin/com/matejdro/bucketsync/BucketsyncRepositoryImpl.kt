package com.matejdro.bucketsync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import app.cash.sqldelight.coroutines.asFlow
import com.matejdro.bucketsync.api.Bucket
import com.matejdro.bucketsync.api.BucketUpdate
import com.matejdro.bucketsync.sqldelight.generated.DbBucketQueries
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dispatch.core.withIO
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import si.inova.kotlinova.core.logging.logcat
import kotlin.time.Duration.Companion.milliseconds

@Inject
@ContributesBinding(AppScope::class)
class BucketsyncRepositoryImpl(
   private val queries: DbBucketQueries,
   private val preferences: DataStore<Preferences>,
) : BucketSyncRepository {
   override suspend fun init(protocolVersion: Int): Boolean {
      val version = preferences.data.first()[lastVersionKey]
      logcat { "Bucket sync init from $version to $protocolVersion" }
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
      logcat { "Update bucket $id" }
      queries.insert(id.toLong(), data)
   }

   override suspend fun awaitNextUpdate(currentVersion: UShort): BucketUpdate = withIO {
      logcat { "Await next update" }
      val versionFlow = queries.getLatestVersion().asFlow().map { it.executeAsOne().MAX?.toUShort() ?: 0u }
      val newVersion = versionFlow.debounce(BUCKET_UPDATE_DEBOUNCE).first { it > currentVersion }

      logcat { "Update $newVersion detected" }

      val bucketsToUpdate = queries.getUpdatedBuckets(currentVersion.toLong()).executeAsList()
      val activeBuckets = queries.getActiveBuckets().executeAsList().map { it.toUShort() }

      logcat { "Active buckets: $activeBuckets, bucketsToUpdate: ${bucketsToUpdate.map { it.id }}" }

      BucketUpdate(
         newVersion,
         activeBuckets,
         bucketsToUpdate.map { Bucket(it.id.toUByte(), it.data_) }
      )
   }

   override suspend fun deleteBucket(id: UShort) = withIO<Unit> {
      logcat { "Delete bucket $id" }
      queries.insert(id.toLong(), null)
   }
}

private val BUCKET_UPDATE_DEBOUNCE = 100.milliseconds
private val lastVersionKey = intPreferencesKey("bucketsync_last_version")
