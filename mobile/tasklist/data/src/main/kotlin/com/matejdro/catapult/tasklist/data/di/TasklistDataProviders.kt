package com.matejdro.catapult.tasklist.data.di

import app.cash.sqldelight.db.SqlDriver
import com.matejdro.catapult.tasklist.sqldelight.generated.Database
import com.matejdro.catapult.tasklist.sqldelight.generated.DbDirectoryQueries
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
interface TasklistDataProviders {
   @Provides
   @SingleIn(AppScope::class)
   fun provideDatabase(driver: SqlDriver): Database {
      return Database(driver)
   }

   @Provides
   fun provideStopQueries(database: Database): DbDirectoryQueries {
      return database.dbDirectoryQueries
   }
}
