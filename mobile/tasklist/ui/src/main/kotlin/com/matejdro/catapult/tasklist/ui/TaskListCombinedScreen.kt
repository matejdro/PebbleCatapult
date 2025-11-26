package com.matejdro.catapult.tasklist.ui

import androidx.compose.runtime.Composable
import com.matejdro.catapult.navigation.keys.FolderTaskListCombinedKey
import com.matejdro.catapult.navigation.keys.TaskListKey
import com.matejdro.catapult.navigation.util.MasterDetailScreen
import com.matejdro.catapult.tasklist.ui.directory.FolderListScreen
import com.matejdro.catapult.tasklist.ui.task.TaskListScreen
import si.inova.kotlinova.navigation.di.ContributesScreenBinding
import si.inova.kotlinova.navigation.screens.InjectNavigationScreen
import si.inova.kotlinova.navigation.screens.Screen

@InjectNavigationScreen
@ContributesScreenBinding(boundType = Screen::class)
class TaskListCombinedScreen(
   private val folderListScreen: FolderListScreen,
   private val taskListScreen: TaskListScreen,
) : MasterDetailScreen<FolderTaskListCombinedKey, TaskListKey>() {
   @Composable
   override fun Master(
      key: FolderTaskListCombinedKey,
      openDetail: (TaskListKey) -> Unit,
   ) {
      folderListScreen.Content { openDetail(TaskListKey(it)) }
   }

   @Composable
   override fun Detail(key: TaskListKey) {
      taskListScreen.Content(key)
   }
}
