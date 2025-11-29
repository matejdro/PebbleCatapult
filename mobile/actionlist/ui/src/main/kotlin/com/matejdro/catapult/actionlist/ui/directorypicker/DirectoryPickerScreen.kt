package com.matejdro.catapult.actionlist.ui.directorypicker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.matejdro.catapult.actionlist.api.CatapultDirectory
import com.matejdro.catapult.actionlist.ui.R
import com.matejdro.catapult.actionlist.ui.directory.DirectoryListState
import com.matejdro.catapult.actionlist.ui.directory.DirectoryListViewModel
import com.matejdro.catapult.ui.components.AlertDialogWithContent
import com.matejdro.catapult.ui.components.ProgressErrorSuccessScaffold
import com.matejdro.catapult.ui.debugging.FullScreenPreviews
import com.matejdro.catapult.ui.debugging.PreviewTheme
import kotlinx.parcelize.Parcelize
import si.inova.kotlinova.compose.components.itemsWithDivider
import si.inova.kotlinova.compose.flow.collectAsStateWithLifecycleAndBlinkingPrevention
import si.inova.kotlinova.core.outcome.Outcome
import si.inova.kotlinova.navigation.screenkeys.ScreenKey
import si.inova.kotlinova.navigation.screens.InjectNavigationScreen
import si.inova.kotlinova.navigation.screens.Screen

@InjectNavigationScreen
class DirectoryPickerScreen(
   private val viewModel: DirectoryListViewModel,
) : Screen<DirectoryPickerKey>() {
   @Composable
   override fun Content(key: DirectoryPickerKey) {
      throw UnsupportedOperationException("This screen is not supported as a standalone")
   }

   @Composable
   fun Content(
      title: String,
      onDirectorySelect: (CatapultDirectory) -> Unit,
      onDismiss: () -> Unit,
      modifier: Modifier = Modifier,
   ) {
      val stateOutcome = viewModel.uiState.collectAsStateWithLifecycleAndBlinkingPrevention().value

      Content(title, stateOutcome, onDismiss, onDirectorySelect, modifier)
   }
}

@Composable
private fun Content(
   title: String,
   stateOutcome: Outcome<DirectoryListState>?,
   onDismiss: () -> Unit,
   onDirectorySelect: (CatapultDirectory) -> Unit,
   modifier: Modifier = Modifier,
) {
   AlertDialogWithContent(
      title = {
         Text(text = title)
      },
      onDismissRequest = {
         onDismiss()
      },
      confirmButton = {},
      dismissButton = {
         TextButton(
            onClick = {
               onDismiss()
            }
         ) {
            Text(stringResource(R.string.cancel))
         }
      },
      modifier = modifier
   ) {
      ProgressErrorSuccessScaffold(stateOutcome) { state ->
         LazyColumn {
            itemsWithDivider(state.directories, key = { it.id }) {
               Text(
                  it.title,
                  Modifier
                     .clickable(onClick = { onDirectorySelect(it) })
                     .padding(8.dp)
                     .fillMaxWidth()
                     .animateItem()
               )
            }
         }
      }
   }
}

@Parcelize
data object DirectoryPickerKey : ScreenKey()

@FullScreenPreviews
@Composable
@ShowkaseComposable(group = "test")
internal fun DirectoryPickerScreenPreview() {
   PreviewTheme {
      Content(
         "Directory Picker",
         Outcome.Success(
            DirectoryListState(
               listOf(
                  CatapultDirectory(1, "Directory 1"),
                  CatapultDirectory(2, "Directory 2"),
                  CatapultDirectory(3, "Directory 3")
               )
            )
         ),
         {}, {},
      )
   }
}
