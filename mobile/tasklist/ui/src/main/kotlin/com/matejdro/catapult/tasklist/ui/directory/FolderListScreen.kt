package com.matejdro.catapult.tasklist.ui.directory

import android.widget.Toast
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.matejdro.catapult.navigation.keys.FolderListKey
import com.matejdro.catapult.tasklist.api.CatapultDirectory
import com.matejdro.catapult.tasklist.ui.R
import com.matejdro.catapult.ui.components.AlertDialogWithContent
import com.matejdro.catapult.ui.components.ProgressErrorSuccessScaffold
import com.matejdro.catapult.ui.debugging.FullScreenPreviews
import com.matejdro.catapult.ui.debugging.PreviewTheme
import si.inova.kotlinova.compose.components.itemsWithDivider
import si.inova.kotlinova.compose.flow.collectAsStateWithLifecycleAndBlinkingPrevention
import si.inova.kotlinova.navigation.di.ContributesScreenBinding
import si.inova.kotlinova.navigation.screens.InjectNavigationScreen
import si.inova.kotlinova.navigation.screens.Screen

@InjectNavigationScreen
@ContributesScreenBinding
class FolderListScreen(
   private val viewModel: FolderListViewModel,
) : Screen<FolderListKey>() {
   @Composable
   override fun Content(key: FolderListKey) {
      val stateOutcome = viewModel.uiState.collectAsStateWithLifecycleAndBlinkingPrevention().value

      var addDialog by remember { mutableStateOf(false) }
      var editDialog by remember { mutableStateOf<Int?>(null) }
      val context = LocalContext.current

      ProgressErrorSuccessScaffold(
         stateOutcome,
         Modifier
            .fillMaxSize()
            .safeDrawingPadding()
      ) { state ->
         FolderListScreenContent(
            state,
            addNew = {
               addDialog = true
            },
            edit = {
               if (it == 1) {
                  Toast.makeText(context, R.string.starting_directory_cannot_be_edited, Toast.LENGTH_SHORT).show()
               } else {
                  editDialog = it
               }
            }
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
            initialText = stateOutcome?.data?.folders?.firstOrNull { it.id == editDialog }?.title.orEmpty(),
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
private fun FolderListScreenContent(state: FolderListState, addNew: () -> Unit, edit: (Int) -> Unit) {
   Box(Modifier.fillMaxSize()) {
      LazyColumn(
         contentPadding = WindowInsets.safeDrawing.asPaddingValues()
      ) {
         itemsWithDivider(state.folders, key = { it.id }) {
            Text(
               it.title,
               Modifier
                  .combinedClickable(onClick = {}, onLongClick = { edit(it.id) })
                  .padding(32.dp)
                  .fillMaxWidth()
                  .animateItem()
            )
         }
      }

      FloatingActionButton(
         onClick = addNew,
         modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(32.dp)
      ) {
         Icon(painterResource(R.drawable.ic_add), stringResource(R.string.add))
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
internal fun FolderListScreenContentPreview() {
   PreviewTheme {
      FolderListScreenContent(
         FolderListState(
            listOf(
               CatapultDirectory(1, "Directory 1"),
               CatapultDirectory(2, "Directory 2"),
               CatapultDirectory(3, "Directory 3")
            )
         ),
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
