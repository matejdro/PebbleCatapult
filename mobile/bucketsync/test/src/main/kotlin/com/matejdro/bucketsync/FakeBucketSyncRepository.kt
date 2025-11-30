package com.matejdro.bucketsync

import androidx.datastore.preferences.core.preferencesOf
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.matejdro.bucketsync.sqldelight.generated.Database
import com.matejdro.bucketsync.sqldelight.generated.DbBucketQueries
import com.matejdro.catapult.common.test.datastore.InMemoryDataStore

fun FakeBucketSyncRepository(): BucketSyncRepository = BucketsyncRepositoryImpl(
   createTestBucketQueries(),
   InMemoryDataStore(preferencesOf())
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
