package com.matejdro.catapult.tasker.ui.screens.actionlist

import com.matejdro.catapult.actionlist.api.CatapultAction
import com.matejdro.catapult.actionlist.api.CatapultDirectory
import com.matejdro.catapult.actionlist.test.FakeCatapultActionRepository
import com.matejdro.catapult.actionlist.test.FakeDirectoryListRepository
import com.matejdro.catapult.navigation.keys.ActionListToggleKey
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import si.inova.kotlinova.core.outcome.Outcome
import si.inova.kotlinova.core.test.outcomes.shouldBeSuccessWithData
import si.inova.kotlinova.core.test.outcomes.testCoroutineResourceManager

class ActionListToggleViewModelTest {
   private val scope = TestScope()
   private val directoryRepo = FakeDirectoryListRepository()
   private val actionsRepo = FakeCatapultActionRepository()

   private val vm = ActionListToggleViewModel(scope.testCoroutineResourceManager(), directoryRepo, actionsRepo, {})

   @Test
   fun `Load directory items`() = scope.runTest {
      initDirectories()
      actionsRepo.insert(CatapultAction("Action A", 1, taskerTaskName = "Task A", id = 1))
      actionsRepo.insert(CatapultAction("Action B", 1, taskerTaskName = "Task B", id = 2))

      vm.key = ActionListToggleKey(1)
      vm.onServiceRegistered()
      runCurrent()

      vm.uiState.value shouldBeSuccessWithData ActionListToggleState(
         CatapultDirectory(1, "Directory"),
         listOf(
            CatapultAction("Action A", 1, taskerTaskName = "Task A", id = 1),
            CatapultAction("Action B", 1, taskerTaskName = "Task B", id = 2),
         ),
         false
      )
   }

   @Test
   fun `Warn when there are more than 13 actions`() = scope.runTest {
      initDirectories()
      repeat(14) {
         actionsRepo.insert(CatapultAction("Action $it", 1, taskerTaskName = "Task A", id = it))
      }

      vm.key = ActionListToggleKey(1)
      vm.onServiceRegistered()
      runCurrent()

      vm.uiState.value.shouldBeInstanceOf<Outcome.Success<ActionListToggleState>>()
         .data
         .showActionsWarning shouldBe true
   }

   private suspend fun initDirectories() {
      directoryRepo.insert(CatapultDirectory(1, "Directory"))
      directoryRepo.insert(CatapultDirectory(2, "Another Directory"))
   }
}
