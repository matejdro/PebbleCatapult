package com.matejdro.catapult.tasklist.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import com.matejdro.catapult.tasklist.api.CatapultDirectory
import com.matejdro.catapult.tasklist.exception.MissingDirectoryException
import com.matejdro.catapult.tasklist.sqldelight.generated.Database
import com.matejdro.catapult.tasklist.sqldelight.generated.DbDirectoryQueries
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import si.inova.kotlinova.core.test.TestScopeWithDispatcherProvider
import si.inova.kotlinova.core.test.outcomes.shouldBeErrorWith
import si.inova.kotlinova.core.test.outcomes.shouldBeSuccessWithData

class DirectoryListRepositoryImplTest {
   private val repo = DirectoryListRepositoryImpl(createTestDirectoryQueries())
   private val scope = TestScopeWithDispatcherProvider()

   @Test
   fun `Return starting directory by default`() = scope.runTest {
      repo.getAll().test {
         runCurrent()
         expectMostRecentItem() shouldBeSuccessWithData listOf(CatapultDirectory(1, "Starting Directory"))
      }
   }

   @Test
   fun `Return added directories`() = scope.runTest {
      repo.getAll().test {
         runCurrent()

         repo.insert(CatapultDirectory(0, "Directory A"))
         runCurrent()

         expectMostRecentItem() shouldBeSuccessWithData listOf(
            CatapultDirectory(1, "Starting Directory"),
            CatapultDirectory(2, "Directory A")
         )
      }
   }

   @Test
   fun `Return a single directory`() = scope.runTest {
      repo.insert(CatapultDirectory(0, "Directory A"))
      runCurrent()

      repo.getSingle(1).test {
         runCurrent()

         expectMostRecentItem() shouldBeSuccessWithData CatapultDirectory(1, "Directory A")
      }
   }

   @Test
   fun `Return outcome with missing directory when requested ID is missing`() = scope.runTest {
      runCurrent()

      repo.getSingle(1).test {
         runCurrent()

         expectMostRecentItem().shouldBeErrorWith(exceptionType = MissingDirectoryException::class.java)
      }
   }

   @Test
   fun `Allow updating directories`() = scope.runTest {
      repo.getAll().test {
         runCurrent()

         repo.insert(CatapultDirectory(0, "Directory A"))
         repo.update(CatapultDirectory(2, "Directory B"))
         runCurrent()

         expectMostRecentItem() shouldBeSuccessWithData listOf(
            CatapultDirectory(1, "Starting Directory"),
            CatapultDirectory(2, "Directory B")
         )
      }
   }

   @Test
   fun `Allow deleting directories`() = scope.runTest {
      repo.getAll().test {
         runCurrent()

         repo.insert(CatapultDirectory(0, "Directory A"))
         repo.delete(2)
         runCurrent()

         expectMostRecentItem() shouldBeSuccessWithData listOf(
            CatapultDirectory(1, "Starting Directory"),
         )
      }
   }

   @Test
   fun `Disallow deleting starting directory`() = scope.runTest {
      assertThrows<IllegalArgumentException> {
         repo.delete(1)
      }
   }

   @Test
   fun `Disallow updating starting directory`() = scope.runTest {
      assertThrows<IllegalArgumentException> {
         repo.update(CatapultDirectory(1, "Nonstarting directory"))
      }
   }
}

internal fun createTestDirectoryQueries(
   driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).apply {
      Database.Schema.create(
         this
      )
   },
): DbDirectoryQueries {
   return Database(driver).dbDirectoryQueries
}
