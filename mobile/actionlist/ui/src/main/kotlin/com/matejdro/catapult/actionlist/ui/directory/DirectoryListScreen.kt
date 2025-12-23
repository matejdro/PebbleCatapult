package com.matejdro.catapult.actionlist.ui.directory

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.matejdro.catapult.actionlist.api.CatapultDirectory
import com.matejdro.catapult.actionlist.ui.R
import com.matejdro.catapult.navigation.instructions.navigateToOrReplaceType
import com.matejdro.catapult.navigation.keys.ActionListKey
import com.matejdro.catapult.navigation.keys.DirectoryListKey
import com.matejdro.catapult.ui.components.AlertDialogWithContent
import com.matejdro.catapult.ui.components.ProgressErrorSuccessScaffold
import com.matejdro.catapult.ui.debugging.FullScreenPreviews
import com.matejdro.catapult.ui.debugging.PreviewTheme
import kotlinx.coroutines.launch
import si.inova.kotlinova.compose.components.itemsWithDivider
import si.inova.kotlinova.compose.flow.collectAsStateWithLifecycleAndBlinkingPrevention
import si.inova.kotlinova.navigation.di.ContributesScreenBinding
import si.inova.kotlinova.navigation.navigator.Navigator
import si.inova.kotlinova.navigation.screens.InjectNavigationScreen
import si.inova.kotlinova.navigation.screens.Screen

@InjectNavigationScreen
@ContributesScreenBinding
class DirectoryListScreen(
   private val viewModel: DirectoryListViewModel,
   private val navigator: Navigator,
) : Screen<DirectoryListKey>() {
   @Composable
   override fun Content(key: DirectoryListKey) {
      Content {
         navigator.navigateToOrReplaceType(ActionListKey(it))
      }
   }

   @Composable
   @Suppress("ModifierMissing") // Full screen
   fun Content(selectDirectory: (Int) -> Unit) {
      val stateOutcome = viewModel.uiState.collectAsStateWithLifecycleAndBlinkingPrevention().value

      var addDialog by rememberSaveable { mutableStateOf(false) }
      var editDialog by rememberSaveable { mutableStateOf<Int?>(null) }
      val context = LocalContext.current

      val scope = rememberCoroutineScope()
      val snackbarHostState = remember { SnackbarHostState() }

      ProgressErrorSuccessScaffold(
         stateOutcome,
         Modifier
            .safeDrawingPadding()
      ) { state ->
         DirectoryListScreenContent(
            state,
            snackbarHostState,
            addNew = {
               if (state.directories.size >= 15) {
                  scope.launch {
                     snackbarHostState.showSnackbar(context.getString(R.string.you_can_only_have_up_to_15_directories))
                  }
               } else {
                  addDialog = true
               }
            },
            edit = {
               if (it == 1) {
                  scope.launch {
                     snackbarHostState.showSnackbar(context.getString(R.string.starting_directory_cannot_be_edited))
                  }
               } else {
                  editDialog = it
               }
            },
            select = selectDirectory
         )
      }

      if (addDialog) {
         NameEntryDialog(
            title = stringResource(R.string.add),
            initialText = "",
            dismiss = { addDialog = false },
            accept = {
               viewModel.add(it)
               addDialog = false
            }
         )
      } else if (editDialog != null) {
         NameEntryDialog(
            title = stringResource(R.string.edit),
            initialText = stateOutcome?.data?.directories?.firstOrNull { it.id == editDialog }?.title.orEmpty(),
            dismiss = { editDialog = null },
            accept = { newTitle ->
               editDialog?.let { id -> viewModel.edit(id, newTitle) }
               editDialog = null
            },
            delete = {
               editDialog?.let { id -> viewModel.delete(id) }
               editDialog = null
            }
         )
      }
   }
}

@Composable
private fun DirectoryListScreenContent(
   state: DirectoryListState,
   snackbarHostState: SnackbarHostState,
   addNew: () -> Unit,
   edit: (Int) -> Unit,
   select: (Int) -> Unit,
) {
   Scaffold(
      Modifier.fillMaxSize(),
      contentWindowInsets = WindowInsets(),
      floatingActionButton = {
         FloatingActionButton(
            onClick = addNew,
            modifier = Modifier
               .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
         ) {
            Icon(painterResource(R.drawable.ic_add), stringResource(R.string.add))
         }
      },
      snackbarHost = { SnackbarHost(snackbarHostState) }
   ) { paddingValues ->
      LazyColumn(
         contentPadding = WindowInsets.safeDrawing.asPaddingValues(),
         modifier = Modifier.padding(paddingValues)
      ) {
         itemsWithDivider(state.directories, key = { it.id }) {
            Text(
               it.title,
               Modifier
                  .combinedClickable(onClick = { select(it.id) }, onLongClick = { edit(it.id) })
                  .padding(32.dp)
                  .fillMaxWidth()
                  .animateItem()
            )
         }
      }
   }
}

@Composable
private fun NameEntryDialog(
   title: String,
   initialText: String,
   dismiss: () -> Unit,
   accept: (text: String) -> Unit,
   delete: (() -> Unit)? = null,
) {
   val textFieldState = rememberTextFieldState(initialText)

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
      }
   ) {
      val focusRequester = remember { FocusRequester() }

      TextField(
         textFieldState,
         Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
         onKeyboardAction = { accept(textFieldState.text.toString()) },
         keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
         lineLimits = TextFieldLineLimits.SingleLine,
      )

      LaunchedEffect(Unit) {
         focusRequester.requestFocus()
      }
   }
}

@FullScreenPreviews
@Composable
@ShowkaseComposable(group = "test")
internal fun DirectoryListScreenContentPreview() {
   PreviewTheme {
      DirectoryListScreenContent(
         DirectoryListState(
            listOf(
               CatapultDirectory(1, "Directory 1"),
               CatapultDirectory(2, "Directory 2"),
               CatapultDirectory(3, "Directory 3")
            )
         ),
         SnackbarHostState(),
         {},
         {},
         {}
      )
   }
}

@Preview
@ShowkaseComposable(group = "test")
@Composable
internal fun NameEntryDialogPreview() {
   PreviewTheme(fill = false) {
      NameEntryDialog(
         title = stringResource(R.string.add),
         initialText = "",
         dismiss = {},
         accept = {}
      )
   }
}
