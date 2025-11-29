package com.matejdro.catapult.tasklist.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import com.matejdro.catapult.tasklist.api.CatapultAction
import com.matejdro.catapult.tasklist.api.CatapultDirectory
import com.matejdro.catapult.tasklist.sqldelight.generated.Database
import com.matejdro.catapult.tasklist.sqldelight.generated.DbActionQueries
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import si.inova.kotlinova.core.test.TestScopeWithDispatcherProvider
import si.inova.kotlinova.core.test.outcomes.shouldBeSuccessWithData

class CatapultActionRepositoryImplTest {
   private val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).apply {
      Database.Schema.create(this)
   }
   private val repo = CatapultActionRepositoryImpl(createTestActionQueries(driver))
   private val scope = TestScopeWithDispatcherProvider()

   @Test
   fun `Return added actions`() = scope.runTest {
      setupDirectories()

      repo.getAll(1).test {
         runCurrent()

         repo.insert(
            CatapultAction("Action A", 1, taskerTaskName = "Task A"),
         )
         repo.insert(
            CatapultAction("Action B", 1, targetDirectoryId = 2)
         )
         runCurrent()

         expectMostRecentItem() shouldBeSuccessWithData listOf(
            CatapultAction("Action A", 1, 1, taskerTaskName = "Task A"),
            CatapultAction("Action B", 1, 2, targetDirectoryId = 2, targetDirectoryName = "Directory B")
         )
      }
   }

   @Test
   fun `Allow updating action name`() = scope.runTest {
      repo.getAll(1).test {
         runCurrent()

         repo.insert(
            CatapultAction("Action A", 1, taskerTaskName = "Task A"),
         )
         runCurrent()

         repo.updateTitle(1, "Action B")
         runCurrent()

         expectMostRecentItem() shouldBeSuccessWithData listOf(
            CatapultAction("Action B", 1, 1, taskerTaskName = "Task A"),
         )
      }
   }

   @Test
   fun `Allow deleting actions`() = scope.runTest {
      repo.getAll(1).test {
         runCurrent()

         repo.insert(
            CatapultAction("Action A", 1, taskerTaskName = "Task A"),
         )
         runCurrent()

         repo.delete(1)
         runCurrent()

         expectMostRecentItem() shouldBeSuccessWithData emptyList()
      }
   }

   @Test
   fun `Move action upwards`() = scope.runTest {
      setupDirectories()

      repo.getAll(1).test {
         runCurrent()

         repo.insert(CatapultAction("Action A", 1, taskerTaskName = "Task A"))
         repo.insert(CatapultAction("Action B", 1, taskerTaskName = "Task B"))
         repo.insert(CatapultAction("Action C", 1, taskerTaskName = "Task C"))
         repo.insert(CatapultAction("Action D", 1, taskerTaskName = "Task D"))
         runCurrent()

         repo.reorder(2, 3)
         runCurrent()

         expectMostRecentItem() shouldBeSuccessWithData listOf(
            CatapultAction("Action A", 1, 1, taskerTaskName = "Task A"),
            CatapultAction("Action C", 1, 3, taskerTaskName = "Task C"),
            CatapultAction("Action D", 1, 4, taskerTaskName = "Task D"),
            CatapultAction("Action B", 1, 2, taskerTaskName = "Task B"),
         )
      }
   }

   @Test
   fun `Move action downwards`() = scope.runTest {
      setupDirectories()

      repo.getAll(1).test {
         runCurrent()

         repo.insert(CatapultAction("Action A", 1, taskerTaskName = "Task A"))
         repo.insert(CatapultAction("Action B", 1, taskerTaskName = "Task B"))
         repo.insert(CatapultAction("Action C", 1, taskerTaskName = "Task C"))
         repo.insert(CatapultAction("Action D", 1, taskerTaskName = "Task D"))
         runCurrent()

         repo.reorder(4, 1)
         runCurrent()

         expectMostRecentItem() shouldBeSuccessWithData listOf(
            CatapultAction("Action A", 1, 1, taskerTaskName = "Task A"),
            CatapultAction("Action D", 1, 4, taskerTaskName = "Task D"),
            CatapultAction("Action B", 1, 2, taskerTaskName = "Task B"),
            CatapultAction("Action C", 1, 3, taskerTaskName = "Task C"),
         )
      }
   }

   @Test
   fun `Handle reordering after delete`() = scope.runTest {
      setupDirectories()

      repo.getAll(1).test {
         runCurrent()

         repo.insert(CatapultAction("Action A", 1, taskerTaskName = "Task A"))
         repo.insert(CatapultAction("Action B", 1, taskerTaskName = "Task B"))
         repo.insert(CatapultAction("Action C", 1, taskerTaskName = "Task C"))
         repo.insert(CatapultAction("Action D", 1, taskerTaskName = "Task D"))
         runCurrent()

         repo.delete(2)
         runCurrent()

         repo.reorder(1, 1)
         runCurrent()

         expectMostRecentItem() shouldBeSuccessWithData listOf(
            CatapultAction("Action C", 1, 3, taskerTaskName = "Task C"),
            CatapultAction("Action A", 1, 1, taskerTaskName = "Task A"),
            CatapultAction("Action D", 1, 4, taskerTaskName = "Task D"),
         )
      }
   }

   @Test
   fun `Do not affect other directories when reordering`() = scope.runTest {
      setupDirectories()

      repo.insert(CatapultAction("Action 1A", 1, taskerTaskName = "Task A"))
      repo.insert(CatapultAction("Action 1B", 1, taskerTaskName = "Task B"))
      repo.insert(CatapultAction("Action 1C", 1, taskerTaskName = "Task C"))
      repo.insert(CatapultAction("Action 1D", 1, taskerTaskName = "Task D"))

      repo.insert(CatapultAction("Action 2A", 2, taskerTaskName = "Task A"))
      repo.insert(CatapultAction("Action 2B", 2, taskerTaskName = "Task B"))
      repo.insert(CatapultAction("Action 2C", 2, taskerTaskName = "Task C"))
      repo.insert(CatapultAction("Action 2D", 2, taskerTaskName = "Task D"))

      runCurrent()

      repo.getAll(1).test {
         repo.reorder(3, 0)
         runCurrent()

         expectMostRecentItem() shouldBeSuccessWithData listOf(
            CatapultAction("Action 1C", 1, 3, taskerTaskName = "Task C"),
            CatapultAction("Action 1A", 1, 1, taskerTaskName = "Task A"),
            CatapultAction("Action 1B", 1, 2, taskerTaskName = "Task B"),
            CatapultAction("Action 1D", 1, 4, taskerTaskName = "Task D"),
         )
      }

      repo.getAll(2).test {
         repo.reorder(5, 1)
         runCurrent()

         expectMostRecentItem() shouldBeSuccessWithData listOf(
            CatapultAction("Action 2B", 2, 6, taskerTaskName = "Task B"),
            CatapultAction("Action 2A", 2, 5, taskerTaskName = "Task A"),
            CatapultAction("Action 2C", 2, 7, taskerTaskName = "Task C"),
            CatapultAction("Action 2D", 2, 8, taskerTaskName = "Task D"),
         )
      }
   }

   private suspend fun setupDirectories() {
      val directoryRepo = DirectoryListRepositoryImpl(createTestDirectoryQueries(driver))

      directoryRepo.insert(CatapultDirectory(0, "Directory A"))
      directoryRepo.insert(CatapultDirectory(1, "Directory B"))
   }
}

private fun createTestActionQueries(
   driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).apply {
      Database.Schema.create(
         this
      )
   },
): DbActionQueries {
   return Database(driver).dbActionQueries
}
