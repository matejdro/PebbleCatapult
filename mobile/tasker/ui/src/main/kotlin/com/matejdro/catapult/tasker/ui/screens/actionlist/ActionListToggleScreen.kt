package com.matejdro.catapult.tasker.ui.screens.actionlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.matejdro.catapult.actionlist.api.CatapultAction
import com.matejdro.catapult.actionlist.api.CatapultDirectory
import com.matejdro.catapult.navigation.keys.ActionListToggleKey
import com.matejdro.catapult.navigation.keys.DirectoryListKey
import com.matejdro.catapult.tasker.BundleKeys
import com.matejdro.catapult.tasker.TaskerAction
import com.matejdro.catapult.tasker.ui.R
import com.matejdro.catapult.tasker.ui.TaskerConfigurationActivity
import com.matejdro.catapult.tasker.ui.errors.taskListUserFriendlyMessage
import com.matejdro.catapult.ui.components.ProgressErrorSuccessScaffold
import com.matejdro.catapult.ui.debugging.FullScreenPreviews
import com.matejdro.catapult.ui.debugging.PreviewTheme
import com.matejdro.catapult.ui.lists.ReorderableListContainer
import si.inova.kotlinova.compose.components.itemsWithDivider
import si.inova.kotlinova.compose.flow.collectAsStateWithLifecycleAndBlinkingPrevention
import si.inova.kotlinova.core.activity.requireActivity
import si.inova.kotlinova.navigation.instructions.replaceTopWith
import si.inova.kotlinova.navigation.navigator.Navigator
import si.inova.kotlinova.navigation.screens.InjectNavigationScreen
import si.inova.kotlinova.navigation.screens.Screen
import com.matejdro.catapult.sharedresources.R as sharedR

@InjectNavigationScreen
class ActionListToggleScreen(
   private val viewModel: ActionListToggleViewModel,
   private val navigator: Navigator,
) : Screen<ActionListToggleKey>() {
   @Composable
   override fun Content(key: ActionListToggleKey) {
      val stateOutcome = viewModel.uiState.collectAsStateWithLifecycleAndBlinkingPrevention().value

      val activity = LocalContext.current.requireActivity().let { it as TaskerConfigurationActivity }

      var actionsToEnable by remember {
         mutableStateOf(
            activity.existingData.getString(BundleKeys.ENABLED_TASK_IDS)
               ?.split(",")?.mapNotNull { it.toIntOrNull() }.orEmpty()
         )
      }

      var actionsToDisable by remember {
         mutableStateOf(
            activity.existingData.getString(BundleKeys.DISABLED_TASK_IDS)
               ?.split(",")?.mapNotNull { it.toIntOrNull() }.orEmpty()
         )
      }

      ProgressErrorSuccessScaffold(
         stateOutcome,
         Modifier
            .safeDrawingPadding(),
         { it.taskListUserFriendlyMessage() }
      ) { state ->
         ActionListToggleScreenContent(
            state = state.copy(
               actions = state.actions.map {
                  val enabled = if (actionsToEnable.contains(it.id)) {
                     true
                  } else if (actionsToDisable.contains(it.id)) {
                     false
                  } else {
                     it.enabled
                  }

                  it.copy(enabled = enabled)
               }
            ),
            toggleActionEnabled = { action, enabled ->
               if (enabled) {
                  actionsToDisable = actionsToDisable.filter { it != action.id }

                  if (!actionsToEnable.contains(action.id)) {
                     actionsToEnable = actionsToEnable + action.id
                  }
               } else {
                  if (!actionsToDisable.contains(action.id)) {
                     actionsToDisable = actionsToDisable + action.id
                  }

                  actionsToEnable = actionsToEnable.filter { it != action.id }
               }

               save(
                  activity,
                  key.directoryId,
                  state,
                  actionsToEnable,
                  actionsToDisable,
               )
            },
            reselectDirectory = {
               activity.clearConfiguration()

               navigator.replaceTopWith(
                  DirectoryListKey(ActionListToggleKey::class.java.name)
               )
            }
         )
      }
   }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionListToggleScreenContent(
   state: ActionListToggleState,
   toggleActionEnabled: (CatapultAction, enabled: Boolean) -> Unit,
   reselectDirectory: () -> Unit,
) {
   Scaffold(
      Modifier.fillMaxSize(),
      contentWindowInsets = WindowInsets(),
   ) { paddingValues ->
      Column(
         Modifier
            .fillMaxWidth()
            .padding(paddingValues)
      ) {
         TopAppBar(title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
               Text(state.directory.title)

               IconButton(onClick = reselectDirectory) {
                  Icon(
                     painterResource(R.drawable.reselect),
                     contentDescription = stringResource(R.string.select_different_directory)
                  )
               }
            }
         })
         HorizontalDivider(color = MaterialTheme.colorScheme.onSurface)

         if (state.showActionsWarning) {
            Text(
               stringResource(sharedR.string.warning_max_actions),
               Modifier
                  .fillMaxWidth()
                  .background(COLOR_WARNING)
                  .padding(16.dp),
            )
         }

         val listState = rememberLazyListState()
         ReorderableListContainer(state.actions, listState) { list ->
            LazyColumn(
               contentPadding = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).asPaddingValues()
            ) {
               itemsWithDivider(list, key = { it.id }) { action ->
                  Row(
                     Modifier
                        .clickable(onClick = { toggleActionEnabled(action, !action.enabled) })
                        .padding(16.dp)
                        .fillMaxWidth()
                        .animateItem(),
                     verticalAlignment = Alignment.CenterVertically
                  ) {
                     Text(
                        action.title,
                        Modifier
                           .weight(1f)
                           .alpha(if (action.enabled) 1f else ALPHA_DISABLED)
                     )

                     Switch(
                        action.enabled,
                        onCheckedChange = {
                           toggleActionEnabled(action, it)
                        },
                     )
                  }
               }

               // Add extra spacer at the end to allow user to scroll around the FAB
               item {
                  Spacer(Modifier.size(100.dp))
               }
            }
         }
      }
   }
}

private fun save(
   activity: TaskerConfigurationActivity,
   directoryId: Int,
   state: ActionListToggleState,
   actionIdsToEnable: List<Int>,
   actionIdsToDisable: List<Int>,
) {
   val actionsToEnable = actionIdsToEnable.mapNotNull { id -> state.actions.find { it.id == id } }
   val actionsToDisable = actionIdsToDisable.mapNotNull { id -> state.actions.find { it.id == id } }

   val textsEnable = actionsToEnable.map { activity.getString(R.string.enable, it.title) }
   val textsDisable = actionsToDisable.map { activity.getString(R.string.disable, it.title) }
   val message = (textsEnable + textsDisable).joinToString(", \n")

   val bundle = bundleOf(
      BundleKeys.ACTION to TaskerAction.TOGGLE_ACTIONS.name,
      BundleKeys.DIRECTORY_ID to directoryId,
      BundleKeys.ENABLED_TASK_IDS to actionsToEnable.map { it.id }.joinToString(","),
      BundleKeys.DISABLED_TASK_IDS to actionsToDisable.map { it.id }.joinToString(","),
   )

   activity.saveConfiguration(bundle, message)
}

private const val ALPHA_DISABLED = 0.75f
private val COLOR_WARNING = Color(0xAAFFA500)

@FullScreenPreviews
@Composable
@ShowkaseComposable(group = "test")
internal fun TaskListToggleScreenContentPreview() {
   PreviewTheme {
      ActionListToggleScreenContent(
         ActionListToggleState(
            CatapultDirectory(1, "Test Directory"),
            listOf(
               CatapultAction("Action A", 1, taskerTaskName = "Task A", id = 1),
               CatapultAction("Action B", 1, taskerTaskName = "Task B", id = 2, enabled = false),
            ),
            false,
         ),
         { _, _ -> },
         {},
      )
   }
}

@Preview
@Composable
@ShowkaseComposable(group = "test")
internal fun TaskListToggleScreenWithFullWarningPreview() {
   PreviewTheme {
      ActionListToggleScreenContent(
         ActionListToggleState(
            CatapultDirectory(1, "Test Directory"),
            listOf(
               CatapultAction("Action A", 1, taskerTaskName = "Task A", id = 1),
               CatapultAction("Action B", 1, taskerTaskName = "Task B", id = 2, enabled = false),
            ),
            true,
         ),
         { _, _ -> },
         {},
      )
   }
}
