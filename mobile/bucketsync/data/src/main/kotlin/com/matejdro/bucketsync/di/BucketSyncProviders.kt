package com.matejdro.bucketsync.di

import app.cash.sqldelight.db.SqlDriver
import com.matejdro.bucketsync.sqldelight.generated.Database
import com.matejdro.bucketsync.sqldelight.generated.DbBucketQueries
import com.matejdro.bucketsync.sqldelight.generated.DbSyncStatusQueries
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
interface BucketSyncProviders {
   @Provides
   @SingleIn(AppScope::class)
   fun provideDatabase(driver: SqlDriver): Database {
      return Database(driver)
   }

   @Provides
   fun provideBucketQueries(database: Database): DbBucketQueries {
      return database.dbBucketQueries
   }

   @Provides
   fun provideSyncStatusQueries(database: Database): DbSyncStatusQueries {
      return database.dbSyncStatusQueries
   }
}
