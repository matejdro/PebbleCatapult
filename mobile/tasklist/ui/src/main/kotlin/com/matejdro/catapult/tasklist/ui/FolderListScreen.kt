package com.matejdro.catapult.tasklist.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.matejdro.catapult.navigation.keys.FolderListKey
import com.matejdro.catapult.tasklist.api.CatapultDirectory
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

      ProgressErrorSuccessScaffold(
         stateOutcome,
         Modifier
            .fillMaxSize()
            .safeDrawingPadding()
      ) {
         FolderListScreenContent(it)
      }
   }
}

@Composable
private fun FolderListScreenContent(state: FolderListState) {
   LazyColumn(
      contentPadding = WindowInsets.safeDrawing.asPaddingValues()
   ) {
      itemsWithDivider(state.folders) {
         Text(it.title, Modifier.padding(32.dp))
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
         )
      )
   }
}
