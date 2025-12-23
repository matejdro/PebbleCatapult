package com.matejdro.catapult.tasker.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import com.matejdro.catapult.common.di.NavigationInjectingApplication
import com.matejdro.catapult.navigation.keys.DirectoryListKey
import com.matejdro.catapult.tasker.BundleKeys
import com.matejdro.catapult.tasker.TaskerPluginConstants
import com.matejdro.catapult.tasker.ui.screens.actionlist.ActionListToggleKey
import com.matejdro.catapult.ui.theme.CatapultTheme
import si.inova.kotlinova.core.state.toMap
import si.inova.kotlinova.navigation.di.NavigationContext
import si.inova.kotlinova.navigation.di.NavigationInjection
import si.inova.kotlinova.navigation.navigation3.NavDisplay
import si.inova.kotlinova.navigation.screenkeys.ScreenKey

abstract class TaskerConfigurationActivity : ComponentActivity() {
   private lateinit var navigationInjectionFactory: NavigationInjection.Factory
   private lateinit var navigationContext: NavigationContext

   lateinit var existingData: Bundle

   override fun onCreate(savedInstanceState: Bundle?) {
      val appGraph = (requireNotNull(application) as NavigationInjectingApplication).applicationGraph
      navigationInjectionFactory = appGraph.getNavigationInjectionFactory()
      navigationContext = appGraph.getNavigationContext()

      super.onCreate(savedInstanceState)

      enableEdgeToEdge()

      println("startingIntent ${intent.extras?.toMap()}")
      existingData = intent?.getBundleExtra(TaskerPluginConstants.EXTRA_BUNDLE) ?: Bundle()

      setContent {
         val existingDirectory = existingData.getInt(BundleKeys.DIRECTORY_ID, -1)
         NavigationRoot(
            listOf(
               if (existingDirectory >= 0) {
                  ActionListToggleKey(existingDirectory)
               } else {
                  DirectoryListKey(ActionListToggleKey::class.java.name)
               }
            )
         )
      }
   }

   @Composable
   private fun NavigationRoot(initialHistory: List<ScreenKey>) {
      CatapultTheme {
         // A surface container using the 'background' color from the theme
         Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
         ) {
            navigationInjectionFactory.NavDisplay(
               initialHistory = { initialHistory },
               entryDecorators = listOf(
                  rememberSaveableStateHolderNavEntryDecorator(),
                  NavEntryDecorator<ScreenKey>(
                     decorate = {
                        Surface {
                           it.Content()
                        }
                     }
                  )
               ),
            )
         }
      }
   }

   fun clearConfiguration() {
      existingData = Bundle()
      setResult(Activity.RESULT_CANCELED)
   }

   fun saveConfiguration(bundle: Bundle, message: String) {
      val intent = Intent().apply {
         putExtra(TaskerPluginConstants.EXTRA_STRING_BLURB, message)
         putExtra(TaskerPluginConstants.EXTRA_BUNDLE, bundle)
      }

      setResult(RESULT_OK, intent)
   }
}
