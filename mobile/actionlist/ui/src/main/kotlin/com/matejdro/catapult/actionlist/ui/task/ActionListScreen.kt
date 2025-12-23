package com.matejdro.catapult.actionlist.ui.task

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Parcelable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.matejdro.catapult.actionlist.api.CatapultAction
import com.matejdro.catapult.actionlist.api.CatapultDirectory
import com.matejdro.catapult.actionlist.ui.R
import com.matejdro.catapult.actionlist.ui.directorypicker.DirectoryPickerScreen
import com.matejdro.catapult.actionlist.ui.errors.taskListUserFriendlyMessage
import com.matejdro.catapult.actionlist.ui.util.MaxStringSizeBytesInputTransformation
import com.matejdro.catapult.navigation.keys.ActionListKey
import com.matejdro.catapult.ui.components.AlertDialogWithContent
import com.matejdro.catapult.ui.components.ProgressErrorSuccessScaffold
import com.matejdro.catapult.ui.debugging.FullScreenPreviews
import com.matejdro.catapult.ui.debugging.PreviewTheme
import com.matejdro.catapult.ui.lists.ReorderableListContainer
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import si.inova.kotlinova.compose.components.itemsWithDivider
import si.inova.kotlinova.compose.flow.collectAsStateWithLifecycleAndBlinkingPrevention
import si.inova.kotlinova.navigation.screens.InjectNavigationScreen
import si.inova.kotlinova.navigation.screens.Screen
import com.matejdro.catapult.sharedresources.R as sharedR

@InjectNavigationScreen
class ActionListScreen(
   private val viewModel: ActionListViewModel,
   private val directoryPicker: DirectoryPickerScreen,
) : Screen<ActionListKey>() {
   @Composable
   override fun Content(key: ActionListKey) {
      val stateOutcome = viewModel.uiState.collectAsStateWithLifecycleAndBlinkingPrevention().value

      LaunchedEffect(key.id) {
         viewModel.load(key.id)
      }
      var addDialog by rememberSaveable { mutableStateOf<AddDialogAction?>(null) }
      var editDialog by remember { mutableStateOf<CatapultAction?>(null) }
      var showDirectoryPicker by remember { mutableStateOf(false) }

      val taskerSelectResult = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
         val taskName = it.data?.dataString ?: return@rememberLauncherForActivityResult
         addDialog = AddDialogAction.TaskerTask(taskName)
      }

      val context = LocalContext.current
      val scope = rememberCoroutineScope()
      val snackbarHostState = remember { SnackbarHostState() }

      ProgressErrorSuccessScaffold(
         stateOutcome,
         Modifier
            .safeDrawingPadding(),
         { it.taskListUserFriendlyMessage() }
      ) { state ->
         TaskListScreenContent(
            state = state,
            snackbarHostState = snackbarHostState,
            addTaskerTask = {
               val taskerSelectIntent = Intent("net.dinglisch.android.tasker.ACTION_TASK_SELECT")
               try {
                  taskerSelectResult.launch(taskerSelectIntent)
               } catch (_: ActivityNotFoundException) {
                  scope.launch {
                     snackbarHostState.showSnackbar(context.getString(R.string.error_no_tasker))
                  }
               }
            },
            editAction = {
               editDialog = it
            },
            addDirectoryLink = {
               showDirectoryPicker = true
            },
            reorderAction = { id, toIndex ->
               viewModel.reorder(id, toIndex)
            },
            toggleActionEnabled = { action, enabled ->
               viewModel.editActionEnabled(action.id, enabled)
            }
         )
      }

      addDialog?.let { action ->
         AddDialog(
            action,
            {
               viewModel.add(
                  title = it,
                  targetTask = (action as? AddDialogAction.TaskerTask)?.name,
                  targetDirectory = (action as? AddDialogAction.Directory)?.id
               )
            },
            { addDialog = null },
         )
      }

      editDialog?.let { action ->
         EditDialog(
            action,
            { viewModel.editActionTitle(action.id, it) },
            { editDialog = null },
            { viewModel.deleteAction(action.id) }
         )
      }

      if (showDirectoryPicker) {
         directoryPicker.Content(
            context.getString(R.string.open_directory),
            onDirectorySelect = {
               addDialog = AddDialogAction.Directory(it.title, it.id)
               showDirectoryPicker = false
            },
            onDismiss = {
               showDirectoryPicker = false
            }
         )
      }
   }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskListScreenContent(
   state: ActionListState,
   snackbarHostState: SnackbarHostState,
   addTaskerTask: () -> Unit,
   addDirectoryLink: () -> Unit,
   editAction: (CatapultAction) -> Unit,
   toggleActionEnabled: (CatapultAction, enabled: Boolean) -> Unit,
   reorderAction: (id: Int, toIndex: Int) -> Unit,
   addButtonsShown: Boolean = false,
) {
   Scaffold(
      Modifier.fillMaxSize(),
      contentWindowInsets = WindowInsets(),
      floatingActionButton = {
         AddButtons(addTaskerTask, addDirectoryLink, addButtonsShown)
      },
      snackbarHost = { SnackbarHost(snackbarHostState) }
   ) { paddingValues ->
      Column(
         Modifier
            .fillMaxWidth()
            .padding(paddingValues)
      ) {
         TopAppBar(title = { Text(state.directory.title) })
         HorizontalDivider(color = MaterialTheme.colorScheme.onSurface)

         if (state.showActionsWarning) {
            Text(
               stringResource(R.string.warning_max_actions),
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
                  ReorderableListItem(
                     action.id,
                     action,
                     setOrder = { reorderAction(action.id, it) },
                  ) { modifier ->
                     Row(
                        modifier
                           .clickable(onClick = { editAction(action) })
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

@Composable
private fun AddButtons(
   addTaskerTask: () -> Unit,
   addDirectoryLink: () -> Unit,
   shown: Boolean = false,
) {
   var showFabs by remember { mutableStateOf(shown) }
   val fabRotation by animateFloatAsState(if (showFabs) ROTATION_QUARTER_CIRCLE_DEG else 0f)

   Column(
      modifier = Modifier
         .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
      horizontalAlignment = Alignment.End,
   ) {
      AnimatedVisibility(
         visible = showFabs,
         enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
         exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
      ) {
         Column(
            Modifier.padding(end = 10.dp),
            horizontalAlignment = Alignment.End,
         ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
               Text(stringResource(R.string.tasker_task))

               FloatingActionButton(
                  containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                  onClick = {
                     showFabs = false
                     addTaskerTask()
                  },
                  modifier = Modifier
                     .padding(start = 8.dp)
                     .size(48.dp)
               ) {
                  Icon(painterResource(R.drawable.ic_cog), stringResource(R.string.add))
               }
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
               Text(stringResource(R.string.open_directory))

               FloatingActionButton(
                  containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                  onClick = {
                     showFabs = false
                     addDirectoryLink()
                  },
                  modifier = Modifier
                     .padding(start = 8.dp)
                     .size(48.dp)
               ) {
                  Icon(painterResource(sharedR.drawable.directories), stringResource(R.string.add))
               }
            }
         }
      }

      FloatingActionButton(
         containerColor = MaterialTheme.colorScheme.tertiaryContainer,
         onClick = {
            showFabs = !showFabs
         },
         modifier = Modifier
            .graphicsLayer {
               rotationZ = fabRotation
            }
            .padding(16.dp)
      ) {
         Icon(painterResource(R.drawable.ic_add), stringResource(R.string.add))
      }
   }
}

@Composable
private fun AddDialog(addingAction: AddDialogAction, confirm: (String) -> Unit, dismissDialog: () -> Unit) {
   val context = LocalContext.current

   ActionEntryDialog(
      title = context.getString(
         when (addingAction) {
            is AddDialogAction.Directory -> R.string.open_directory
            is AddDialogAction.TaskerTask -> R.string.tasker_task
         }
      ),
      initialText = addingAction.title,
      actionPrefixText = when (addingAction) {
         is AddDialogAction.Directory -> stringResource(R.string.will_open_a_directory)
         is AddDialogAction.TaskerTask -> stringResource(R.string.will_start_a_tasker_task)
      },
      actionNameText = addingAction.title,
      dismiss = dismissDialog,
      accept = {
         confirm(it)
         dismissDialog()
      }
   )
}

@Composable
private fun EditDialog(editingAction: CatapultAction, confirm: (String) -> Unit, dismissDialog: () -> Unit, delete: () -> Unit) {
   val context = LocalContext.current

   ActionEntryDialog(
      title = context.getString(
         if (editingAction.taskerTaskName != null) {
            R.string.tasker_task
         } else {
            R.string.open_directory
         }
      ),
      initialText = editingAction.title,
      actionPrefixText = context.getString(
         if (editingAction.taskerTaskName != null) {
            R.string.will_start_a_tasker_task
         } else {
            R.string.will_open_a_directory
         }
      ),
      actionNameText = editingAction.taskerTaskName ?: editingAction.targetDirectoryName.orEmpty(),
      dismiss = {
         dismissDialog()
      },
      accept = {
         confirm(it)
         dismissDialog()
      },
      delete = {
         delete()
         dismissDialog()
      }
   )
}

@Composable
private fun ActionEntryDialog(
   title: String,
   initialText: String,
   actionPrefixText: String,
   actionNameText: String,
   dismiss: () -> Unit,
   accept: (text: String) -> Unit,
   delete: (() -> Unit)? = null,
) {
   val inputTransformation = remember { MaxStringSizeBytesInputTransformation(MAX_ACTION_TITLE_BYTES) }
   val textFieldState = rememberTextFieldState(inputTransformation.trim(initialText))

   AlertDialogWithContent(
      title = {
         Text(text = title)
      },
      onDismissRequest = {
         dismiss()
      },
      confirmButton = {
         TextButton(
            onClick = {
               accept(textFieldState.text.toString())
            }
         ) {
            Text(stringResource(R.string.ok))
         }
      },
      dismissButton = {
         TextButton(
            onClick = {
               dismiss()
            }
         ) {
            Text(stringResource(R.string.cancel))
         }
      },
      neutralButton = {
         if (delete != null) {
            TextButton(
               onClick = {
                  delete()
               }
            ) {
               Text(stringResource(R.string.delete))
            }
         }
      },
   ) {
      val focusRequester = remember { FocusRequester() }

      Column {
         TextField(
            textFieldState,
            Modifier
               .fillMaxWidth()
               .focusRequester(focusRequester)
               .padding(bottom = 8.dp),
            onKeyboardAction = { accept(textFieldState.text.toString()) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            lineLimits = TextFieldLineLimits.SingleLine,
            inputTransformation = inputTransformation
         )

         FlowRow {
            Text(actionPrefixText)
            Text(actionNameText, fontWeight = FontWeight.Bold)
         }
      }

      LaunchedEffect(Unit) {
         focusRequester.requestFocus()
      }
   }
}

private const val MAX_ACTION_TITLE_BYTES = 14

private sealed class AddDialogAction : Parcelable {
   abstract val title: String

   @Parcelize
   class TaskerTask(val name: String) : AddDialogAction() {
      override val title: String
         get() = name
   }

   @Parcelize
   class Directory(val name: String, val id: Int) : AddDialogAction() {
      override val title: String
         get() = name
   }
}

private const val ROTATION_QUARTER_CIRCLE_DEG = 45f
private const val ALPHA_DISABLED = 0.75f
private val COLOR_WARNING = Color(0xAAFFA500)

@FullScreenPreviews
@Composable
@ShowkaseComposable(group = "test")
internal fun TaskListScreenContentPreview() {
   PreviewTheme {
      TaskListScreenContent(
         ActionListState(
            CatapultDirectory(1, "Test Directory"),
            listOf(
               CatapultAction("Action A", 1, taskerTaskName = "Task A", id = 1),
               CatapultAction("Action B", 1, taskerTaskName = "Task B", id = 2, enabled = false),
            ),
            false,
         ),
         SnackbarHostState(),
         {},
         {},
         {},
         { _, _ -> },
         { _, _ -> },
      )
   }
}

@Preview
@Composable
@ShowkaseComposable(group = "test")
internal fun TaskListScreenAddPreview() {
   PreviewTheme {
      TaskListScreenContent(
         ActionListState(
            CatapultDirectory(1, "Test Directory"),
            listOf(
               CatapultAction("Action A", 1, taskerTaskName = "Task A", id = 1),
               CatapultAction("Action B", 1, taskerTaskName = "Task B", id = 2),
            ),
            false
         ),
         SnackbarHostState(),
         {},
         {},
         {},
         { _, _ -> },
         { _, _ -> },
         addButtonsShown = true
      )
   }
}

@Preview
@Composable
@ShowkaseComposable(group = "test")
internal fun TaskListScreenWithFullWarningPreview() {
   PreviewTheme {
      TaskListScreenContent(
         ActionListState(
            CatapultDirectory(1, "Test Directory"),
            listOf(
               CatapultAction("Action A", 1, taskerTaskName = "Task A", id = 1),
               CatapultAction("Action B", 1, taskerTaskName = "Task B", id = 2, enabled = false),
            ),
            true,
         ),
         SnackbarHostState(),
         {},
         {},
         {},
         { _, _ -> },
         { _, _ -> },
      )
   }
}
