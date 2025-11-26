package com.matejdro.catapult.home.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.matejdro.catapult.home.ui.R
import com.matejdro.catapult.navigation.keys.FolderTaskListCombinedKey
import com.matejdro.catapult.navigation.keys.HomeScreenKey
import com.matejdro.catapult.navigation.keys.ToolsScreenKey
import com.matejdro.catapult.ui.debugging.FullScreenPreviews
import com.matejdro.catapult.ui.debugging.PreviewTheme
import si.inova.kotlinova.core.activity.requireActivity
import si.inova.kotlinova.navigation.instructions.navigateTo
import si.inova.kotlinova.navigation.navigator.Navigator
import si.inova.kotlinova.navigation.screenkeys.ScreenKey
import si.inova.kotlinova.navigation.screens.InjectNavigationScreen
import si.inova.kotlinova.navigation.screens.Screen

@InjectNavigationScreen
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
class HomeScreen(
   private val navigator: Navigator,
   private val foldersScreen: Screen<FolderTaskListCombinedKey>,
   private val toolsScreen: Screen<ToolsScreenKey>,
) : Screen<HomeScreenKey>() {
   @Composable
   override fun Content(key: HomeScreenKey) {
      val keyState = rememberUpdatedState(key)

      val mainContent = remember {
         movableContentOf {
            MainContent(keyState.value.selectedScreen)
         }
      }

      val sizeClass = calculateWindowSizeClass(activity = LocalContext.current.requireActivity())

      Surface {
         HomeScreenContent(
            selectedScreen = key.selectedScreen,
            tabletMode = sizeClass.widthSizeClass == WindowWidthSizeClass.Expanded,
            mainContent = mainContent,
            switchScreen = { navigator.navigateTo(HomeScreenKey(it)) },
         )
      }
   }

   @Composable
   private fun MainContent(tab: ScreenKey) {
      val stateHolder = rememberSaveableStateHolder()
      // We must provide name here, not the enum, because name stays the same after process kill, while enum object is different
      stateHolder.SaveableStateProvider(tab.javaClass.name) {
         when (tab) {
            is FolderTaskListCombinedKey -> foldersScreen.Content(tab)
            is ToolsScreenKey -> toolsScreen.Content(tab)
            else -> error("Unhandled screen type $tab")
         }
      }
   }
}

@Composable
private fun HomeScreenContent(
   selectedScreen: ScreenKey,
   tabletMode: Boolean,
   mainContent: @Composable () -> Unit,
   switchScreen: (ScreenKey) -> Unit,
) {
   if (tabletMode) {
      NavigationRailContent(mainContent, selectedScreen, switchScreen)
   } else {
      NavigationBarContent(mainContent, selectedScreen, switchScreen)
   }
}

@Composable
private fun NavigationBarContent(
   mainContent: @Composable () -> Unit,
   selectedScreen: ScreenKey,
   switchScreen: (ScreenKey) -> Unit,
) {
   Column {
      Box(
         Modifier
            .fillMaxWidth()
            .weight(1f)
            .consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
      ) {
         mainContent()
      }

      NavigationBar {
         NavigationBarItem(
            selected = selectedScreen is FolderTaskListCombinedKey,
            onClick = { switchScreen(FolderTaskListCombinedKey) },
            icon = { Icon(painter = painterResource(id = R.drawable.folders), contentDescription = null) },
            label = { Text(stringResource(R.string.folders)) }
         )

         NavigationBarItem(
            selected = selectedScreen is ToolsScreenKey,
            onClick = { switchScreen(ToolsScreenKey) },
            icon = { Icon(painter = painterResource(id = R.drawable.tools), contentDescription = null) },
            label = { Text(stringResource(R.string.tools)) }
         )
      }
   }
}

@Composable
private fun NavigationRailContent(
   mainContent: @Composable () -> Unit,
   selectedScreen: ScreenKey,
   switchScreen: (ScreenKey) -> Unit,
) {
   Row {
      NavigationRail {
         NavigationRailItem(
            selected = selectedScreen is FolderTaskListCombinedKey,
            onClick = { switchScreen(FolderTaskListCombinedKey) },
            icon = { Icon(painter = painterResource(id = R.drawable.folders), contentDescription = null) },
            label = { Text(stringResource(R.string.folders)) }
         )

         NavigationRailItem(
            selected = selectedScreen is ToolsScreenKey,
            onClick = { switchScreen(ToolsScreenKey) },
            icon = { Icon(painter = painterResource(id = R.drawable.tools), contentDescription = null) },
            label = { Text(stringResource(R.string.tools)) }
         )
      }

      Box(
         Modifier
            .fillMaxHeight()
            .weight(1f)
      ) {
         mainContent()
      }
   }
}

@FullScreenPreviews
@Composable
@ShowkaseComposable(group = "Test")
internal fun HomePhonePreview() {
   PreviewTheme {
      HomeScreenContent(
         tabletMode = false,
         selectedScreen = FolderTaskListCombinedKey,
         mainContent = {
            Box(
               Modifier
                  .fillMaxSize()
                  .background(Color.Red)
            )
         },
         switchScreen = {},
      )
   }
}

@Preview(device = Devices.TABLET)
@Composable
@ShowkaseComposable(group = "Test")
internal fun HomeTabletPreview() {
   PreviewTheme {
      HomeScreenContent(
         tabletMode = true,
         selectedScreen = FolderTaskListCombinedKey,
         mainContent = {
            Box(
               Modifier
                  .fillMaxSize()
                  .background(Color.Red)
            )
         },
         switchScreen = {},
      )
   }
}
