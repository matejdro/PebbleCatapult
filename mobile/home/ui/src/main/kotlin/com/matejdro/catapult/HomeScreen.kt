package com.matejdro.catapult

import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.matejdro.catapult.navigation.keys.HomeScreenKey
import si.inova.kotlinova.navigation.screens.InjectNavigationScreen
import si.inova.kotlinova.navigation.screens.Screen

@InjectNavigationScreen
class HomeScreen : Screen<HomeScreenKey>() {
   @Composable
   override fun Content(key: HomeScreenKey) {
      Surface(Modifier.safeDrawingPadding()) {
         Text("Hello World")
      }
   }
}
