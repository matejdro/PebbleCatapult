package com.matejdro.catapult.actionlist.data.di

import app.cash.sqldelight.db.SqlDriver
import com.matejdro.catapult.actionlist.sqldelight.generated.Database
import com.matejdro.catapult.actionlist.sqldelight.generated.DbActionQueries
import com.matejdro.catapult.actionlist.sqldelight.generated.DbDirectoryQueries
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
interface ActionDataProviders {
   @Provides
   @SingleIn(AppScope::class)
   fun provideDatabase(driver: SqlDriver): Database {
      return Database(driver)
   }

   @Provides
   fun provideDirectoryQueries(database: Database): DbDirectoryQueries {
      return database.dbDirectoryQueries
   }

   @Provides
   fun provideActionQueries(database: Database): DbActionQueries {
      return database.dbActionQueries
   }
}
