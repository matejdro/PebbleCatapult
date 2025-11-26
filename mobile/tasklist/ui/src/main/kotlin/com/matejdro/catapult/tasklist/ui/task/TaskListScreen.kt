package com.matejdro.catapult.tasklist.ui.task

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.matejdro.catapult.navigation.keys.TaskListKey
import com.matejdro.catapult.tasklist.api.CatapultDirectory
import com.matejdro.catapult.tasklist.ui.errors.taskListUserFriendlyMessage
import com.matejdro.catapult.ui.components.ProgressErrorSuccessScaffold
import com.matejdro.catapult.ui.debugging.FullScreenPreviews
import com.matejdro.catapult.ui.debugging.PreviewTheme
import si.inova.kotlinova.compose.flow.collectAsStateWithLifecycleAndBlinkingPrevention
import si.inova.kotlinova.navigation.screens.InjectNavigationScreen
import si.inova.kotlinova.navigation.screens.Screen

@InjectNavigationScreen
class TaskListScreen(
   private val viewModel: TaskListViewModel,
) : Screen<TaskListKey>() {
   @Composable
   override fun Content(key: TaskListKey) {
      val stateOutcome = viewModel.uiState.collectAsStateWithLifecycleAndBlinkingPrevention().value

      LaunchedEffect(key.id) {
         viewModel.load(key.id)
      }

      ProgressErrorSuccessScaffold(
         stateOutcome,
         Modifier
            .safeDrawingPadding(),
         { it.taskListUserFriendlyMessage() }
      ) { state ->
         TaskListScreenContent(state)
      }
   }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskListScreenContent(state: TaskListState) {
   Column {
      TopAppBar(title = { Text(state.directory.title) })
      HorizontalDivider(color = MaterialTheme.colorScheme.onSurface)
   }
}

@FullScreenPreviews
@Composable
private fun TaskListScreenContentPreview() {
   PreviewTheme {
      TaskListScreenContent(TaskListState(CatapultDirectory(1, "Test Directory")))
   }
}
