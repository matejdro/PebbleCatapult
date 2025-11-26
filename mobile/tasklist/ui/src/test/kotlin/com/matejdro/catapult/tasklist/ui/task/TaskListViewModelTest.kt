package com.matejdro.catapult.tasklist.ui.task

import com.matejdro.catapult.tasklist.api.CatapultDirectory
import com.matejdro.catapult.tasklist.test.FakeDirectoryListRepository
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import si.inova.kotlinova.core.test.outcomes.shouldBeSuccessWithData
import si.inova.kotlinova.core.test.outcomes.testCoroutineResourceManager

class TaskListViewModelTest {
   private val scope = TestScope()
   private val directoryRepo = FakeDirectoryListRepository()

   private val vm = TaskListViewModel(scope.testCoroutineResourceManager(), directoryRepo, {})

   @Test
   fun `Load directory items`() = scope.runTest {
      directoryRepo.insert(CatapultDirectory(1, "Directory"))

      vm.load(1)
      runCurrent()

      vm.uiState.value shouldBeSuccessWithData TaskListState(CatapultDirectory(1, "Directory"))
   }

   @Test
   fun `Allow switching to a different directory`() = scope.runTest {
      directoryRepo.insert(CatapultDirectory(1, "Directory"))
      directoryRepo.insert(CatapultDirectory(2, "Another Directory"))

      vm.load(1)
      runCurrent()
      vm.load(2)
      runCurrent()

      vm.uiState.value shouldBeSuccessWithData TaskListState(CatapultDirectory(2, "Another Directory"))
   }

   @Test
   fun `Do not reset flows when collecting multiple ones`() = scope.runTest {
      directoryRepo.insert(CatapultDirectory(1, "Directory"))

      repeat(2) {
         vm.load(1)
         runCurrent()
      }

      directoryRepo.numCollections shouldBe 1
   }
}
