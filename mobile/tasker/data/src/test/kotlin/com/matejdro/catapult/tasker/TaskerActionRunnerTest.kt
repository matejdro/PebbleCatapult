package com.matejdro.catapult.tasker

import androidx.core.os.bundleOf
import com.matejdro.catapult.actionlist.api.CatapultAction
import com.matejdro.catapult.actionlist.test.FakeCatapultActionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import si.inova.kotlinova.core.test.outcomes.shouldBeSuccessWithData

class TaskerActionRunnerTest {
   private val repo = FakeCatapultActionRepository()
   private val runner = TaskerActionRunner(repo)

   @Test
   fun `Run toggle action`() = runTest {
      repo.insert(
         CatapultAction("Action A", 10, 1, enabled = false),
         CatapultAction("Action B", 10, 2, enabled = false),
         CatapultAction("Action C", 10, 3, enabled = true),
         CatapultAction("Action D", 10, 4, enabled = true),
      )

      runner.run(
         bundleOf(
            BundleKeys.ACTION to "TOGGLE_ACTIONS",
            BundleKeys.DIRECTORY_ID to 10,
            BundleKeys.ENABLED_TASK_IDS to "1,2",
            BundleKeys.DISABLED_TASK_IDS to "3,4"
         )
      )
      runCurrent()

      repo.getAll(10).first() shouldBeSuccessWithData listOf(
         CatapultAction("Action A", 10, 1, enabled = true),
         CatapultAction("Action B", 10, 2, enabled = true),
         CatapultAction("Action C", 10, 3, enabled = false),
         CatapultAction("Action D", 10, 4, enabled = false),
      )
   }
}
