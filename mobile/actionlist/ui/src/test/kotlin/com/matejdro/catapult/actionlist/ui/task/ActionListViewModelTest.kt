package com.matejdro.catapult.actionlist.ui.task

import com.matejdro.catapult.actionlist.api.CatapultAction
import com.matejdro.catapult.actionlist.api.CatapultDirectory
import com.matejdro.catapult.actionlist.test.FakeCatapultActionRepository
import com.matejdro.catapult.actionlist.test.FakeDirectoryListRepository
import com.matejdro.catapult.navigation.keys.ActionListKey
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import si.inova.kotlinova.core.outcome.Outcome
import si.inova.kotlinova.core.test.outcomes.shouldBeSuccessWithData
import si.inova.kotlinova.core.test.outcomes.testCoroutineResourceManager

class ActionListViewModelTest {
   private val scope = TestScope()
   private val directoryRepo = FakeDirectoryListRepository()
   private val actionsRepo = FakeCatapultActionRepository()

   private val vm = ActionListViewModel(scope.testCoroutineResourceManager(), directoryRepo, actionsRepo, {})

   @Test
   fun `Load directory items`() = scope.runTest {
      initDirectories()
      actionsRepo.insert(CatapultAction("Action A", 1, taskerTaskName = "Task A", id = 1))
      actionsRepo.insert(CatapultAction("Action B", 1, taskerTaskName = "Task B", id = 2))

      vm.key = ActionListKey(1)
      vm.onServiceRegistered()
      runCurrent()

      vm.uiState.value shouldBeSuccessWithData ActionListState(
         CatapultDirectory(1, "Directory"),
         listOf(
            CatapultAction("Action A", 1, taskerTaskName = "Task A", id = 1),
            CatapultAction("Action B", 1, taskerTaskName = "Task B", id = 2),
         ),
         false
      )
   }

   @Test
   fun `Allow adding new Tasker task`() = scope.runTest {
      initDirectories()

      vm.key = ActionListKey(1)
      vm.onServiceRegistered()
      vm.add("A Task", "My Task", null)
      runCurrent()

      actionsRepo.getAll(1).first() shouldBeSuccessWithData listOf(
         CatapultAction("A Task", directoryId = 1, id = 0, taskerTaskName = "My Task")
      )
   }

   @Test
   fun `Allow adding new Directory link`() = scope.runTest {
      initDirectories()

      vm.key = ActionListKey(1)
      vm.onServiceRegistered()
      vm.add("A Task", null, 2)
      runCurrent()

      actionsRepo.getAll(1).first() shouldBeSuccessWithData listOf(
         CatapultAction("A Task", directoryId = 1, id = 0, targetDirectoryId = 2)
      )
   }

   @Test
   fun `Allow editing name of the existing action`() = scope.runTest {
      initDirectories()
      actionsRepo.insert(CatapultAction("Action A", 1, taskerTaskName = "Task A", id = 1))
      actionsRepo.insert(CatapultAction("Action B", 1, taskerTaskName = "Task B", id = 2))

      vm.key = ActionListKey(1)
      vm.onServiceRegistered()
      runCurrent()
      vm.editActionTitle(2, "Action C")
      runCurrent()

      actionsRepo.getAll(1).first() shouldBeSuccessWithData listOf(
         CatapultAction("Action A", 1, taskerTaskName = "Task A", id = 1),
         CatapultAction("Action C", 1, taskerTaskName = "Task B", id = 2)
      )
   }

   @Test
   fun `Allow editing the enabled status of the existing action`() = scope.runTest {
      initDirectories()
      actionsRepo.insert(CatapultAction("Action A", 1, taskerTaskName = "Task A", id = 1))
      actionsRepo.insert(CatapultAction("Action B", 1, taskerTaskName = "Task B", id = 2))

      vm.key = ActionListKey(1)
      vm.onServiceRegistered()
      runCurrent()
      vm.editActionEnabled(2, false)
      runCurrent()

      actionsRepo.getAll(1).first() shouldBeSuccessWithData listOf(
         CatapultAction("Action A", 1, taskerTaskName = "Task A", id = 1),
         CatapultAction("Action B", 1, taskerTaskName = "Task B", id = 2, enabled = false)
      )
   }

   @Test
   fun `Allow deleting the existing action`() = scope.runTest {
      initDirectories()
      actionsRepo.insert(CatapultAction("Action A", 1, taskerTaskName = "Task A", id = 1))
      actionsRepo.insert(CatapultAction("Action B", 1, taskerTaskName = "Task B", id = 2))

      vm.key = ActionListKey(1)
      vm.onServiceRegistered()
      vm.deleteAction(1)
      runCurrent()

      actionsRepo.getAll(1).first() shouldBeSuccessWithData listOf(
         CatapultAction("Action B", 1, taskerTaskName = "Task B", id = 2)
      )
   }

   @Test
   fun `Allow reordering name of the existing actions`() = scope.runTest {
      initDirectories()
      actionsRepo.insert(CatapultAction("Action A", 1, taskerTaskName = "Task A", id = 1))
      actionsRepo.insert(CatapultAction("Action B", 1, taskerTaskName = "Task B", id = 2))

      vm.key = ActionListKey(1)
      vm.onServiceRegistered()
      vm.reorder(1, 1)
      runCurrent()

      actionsRepo.getAll(1).first() shouldBeSuccessWithData listOf(
         CatapultAction("Action B", 1, taskerTaskName = "Task B", id = 2),
         CatapultAction("Action A", 1, taskerTaskName = "Task A", id = 1),
      )
   }

   @Test
   fun `Warn when there are more than 13 actions`() = scope.runTest {
      initDirectories()
      repeat(14) {
         actionsRepo.insert(CatapultAction("Action $it", 1, taskerTaskName = "Task A", id = it))
      }

      vm.key = ActionListKey(1)
      vm.onServiceRegistered()
      runCurrent()

      vm.uiState.value.shouldBeInstanceOf<Outcome.Success<ActionListState>>()
         .data
         .showActionsWarning shouldBe true
   }

   private suspend fun initDirectories() {
      directoryRepo.insert(CatapultDirectory(1, "Directory"))
      directoryRepo.insert(CatapultDirectory(2, "Another Directory"))
   }
}
