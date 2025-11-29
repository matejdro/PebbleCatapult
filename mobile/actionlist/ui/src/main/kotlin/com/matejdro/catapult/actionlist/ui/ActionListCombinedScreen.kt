package com.matejdro.catapult.actionlist.ui

import androidx.compose.runtime.Composable
import com.matejdro.catapult.actionlist.ui.directory.DirectoryListScreen
import com.matejdro.catapult.actionlist.ui.task.ActionListScreen
import com.matejdro.catapult.navigation.keys.ActionListKey
import com.matejdro.catapult.navigation.keys.DirectoryTaskListCombinedKey
import com.matejdro.catapult.navigation.util.MasterDetailScreen
import si.inova.kotlinova.navigation.di.ContributesScreenBinding
import si.inova.kotlinova.navigation.screens.InjectNavigationScreen
import si.inova.kotlinova.navigation.screens.Screen

@InjectNavigationScreen
@ContributesScreenBinding(boundType = Screen::class)
class ActionListCombinedScreen(
   private val directoryListScreen: DirectoryListScreen,
   private val actionListScreen: ActionListScreen,
) : MasterDetailScreen<DirectoryTaskListCombinedKey, ActionListKey>() {
   @Composable
   override fun Master(
      key: DirectoryTaskListCombinedKey,
      openDetail: (ActionListKey) -> Unit,
   ) {
      directoryListScreen.Content { openDetail(ActionListKey(it)) }
   }

   @Composable
   override fun Detail(key: ActionListKey) {
      actionListScreen.Content(key)
   }
}
