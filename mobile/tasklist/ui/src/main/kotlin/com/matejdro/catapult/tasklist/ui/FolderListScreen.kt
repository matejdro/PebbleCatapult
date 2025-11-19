package com.matejdro.catapult.tasklist.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.matejdro.catapult.navigation.keys.FolderListKey
import si.inova.kotlinova.navigation.di.ContributesScreenBinding
import si.inova.kotlinova.navigation.screens.InjectNavigationScreen
import si.inova.kotlinova.navigation.screens.Screen

@InjectNavigationScreen
@ContributesScreenBinding
class FolderListScreen : Screen<FolderListKey>() {
   @Composable
   override fun Content(key: FolderListKey) {
      Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
         Text("Hello World")
      }
   }
}
