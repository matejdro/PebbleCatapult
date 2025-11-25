package com.matejdro.catapult.tasklist.ui

import com.matejdro.catapult.tasklist.api.CatapultDirectory
import com.matejdro.catapult.tasklist.test.FakeDirectoryListRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import si.inova.kotlinova.core.test.outcomes.shouldBeSuccessWithData
import si.inova.kotlinova.core.test.outcomes.testCoroutineResourceManager

class FolderListViewModelTest {
   private val repo = FakeDirectoryListRepository()
   private val scope = TestScope()

   private val vm = FolderListViewModel(
      repo,
      scope.testCoroutineResourceManager(),
      {}
   )

   @Test
   fun `Pass directories to the UI State`() = scope.runTest {
      repo.insertDirectory(CatapultDirectory(1, "Hello Directory"))

      vm.onServiceRegistered()
      runCurrent()

      vm.uiState.value shouldBeSuccessWithData FolderListState(listOf(CatapultDirectory(1, "Hello Directory")))
   }

   @Test
   fun `Add should add a directory to the repo`() = scope.runTest {
      vm.add("New Directory")
      runCurrent()

      repo.getAllDirectories().first() shouldBeSuccessWithData listOf(CatapultDirectory(0, "New Directory"))
   }
}
