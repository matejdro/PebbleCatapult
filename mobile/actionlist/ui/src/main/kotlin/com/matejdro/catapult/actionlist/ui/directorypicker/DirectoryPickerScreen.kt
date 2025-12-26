package com.matejdro.catapult.actionlist.ui.directorypicker

import android.os.Parcelable
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
import androidx.compose.ui.window.DialogProperties
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
import si.inova.kotlinova.compose.result.LocalResultPassingStore
import si.inova.kotlinova.compose.result.ResultKey
import si.inova.kotlinova.core.outcome.Outcome
import si.inova.kotlinova.navigation.instructions.goBack
import si.inova.kotlinova.navigation.navigator.Navigator
import si.inova.kotlinova.navigation.screenkeys.DialogKey
import si.inova.kotlinova.navigation.screenkeys.ScreenKey
import si.inova.kotlinova.navigation.screens.InjectNavigationScreen
import si.inova.kotlinova.navigation.screens.Screen

@InjectNavigationScreen
class DirectoryPickerScreen(
   private val viewModel: DirectoryListViewModel,
   private val navigator: Navigator,
) : Screen<DirectoryPickerKey>() {
   @Composable
   override fun Content(key: DirectoryPickerKey) {
      val stateOutcome = viewModel.uiState.collectAsStateWithLifecycleAndBlinkingPrevention().value
      val resultPassingStore = LocalResultPassingStore.current

      Content(
         stateOutcome = stateOutcome,
         onDismiss = { navigator.goBack() },
         onDirectorySelect = {
            resultPassingStore.sendResult(key.result, DirectoryPickerKey.Result(it.id, it.title))
            navigator.goBack()
         }
      )
   }
}

@Composable
private fun Content(
   stateOutcome: Outcome<DirectoryListState>?,
   onDismiss: () -> Unit,
   onDirectorySelect: (CatapultDirectory) -> Unit,
   modifier: Modifier = Modifier,
) {
   AlertDialogWithContent(
      title = {
         Text(text = stringResource(R.string.open_directory))
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
data class DirectoryPickerKey(
   val result: ResultKey<Result>,
) : ScreenKey(), DialogKey {
   override val dialogProperties: DialogProperties
      get() = DialogProperties()

   @Parcelize
   data class Result(val id: Int, val title: String) : Parcelable
}

@FullScreenPreviews
@Composable
@ShowkaseComposable(group = "test")
internal fun DirectoryPickerScreenPreview() {
   PreviewTheme {
      Content(
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
