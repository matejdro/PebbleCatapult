package com.matejdro.bucketsync

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.matejdro.bucketsync.background.FakeBackgroundSyncNotifier
import com.matejdro.bucketsync.sqldelight.generated.Database
import com.matejdro.bucketsync.sqldelight.generated.DbBucketQueries
import com.matejdro.catapult.common.test.datastore.InMemoryDataStore

fun FakeBucketSyncRepository(
   previousProtocolVersion: Int = 0,
): BucketSyncRepository = BucketsyncRepositoryImpl(
   createTestBucketQueries(),
   InMemoryDataStore(
      preferencesOf(intPreferencesKey("bucketsync_last_version") to previousProtocolVersion)
   ),
   FakeBackgroundSyncNotifier()
)

private fun createTestBucketQueries(
   driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.Companion.IN_MEMORY).apply {
      Database.Companion.Schema.create(
         this
      )
   },
): DbBucketQueries {
   return Database.Companion(driver).dbBucketQueries
}
