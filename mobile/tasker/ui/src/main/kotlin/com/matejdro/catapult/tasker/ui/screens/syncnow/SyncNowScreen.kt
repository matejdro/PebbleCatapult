package com.matejdro.catapult.tasker.ui.screens.syncnow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.matejdro.catapult.tasker.BundleKeys
import com.matejdro.catapult.tasker.TaskerAction
import com.matejdro.catapult.tasker.ui.R
import com.matejdro.catapult.tasker.ui.TaskerConfigurationActivity
import com.matejdro.catapult.ui.debugging.FullScreenPreviews
import com.matejdro.catapult.ui.debugging.PreviewTheme
import kotlinx.parcelize.Parcelize
import me.zhanghai.compose.preference.LocalPreferenceTheme
import me.zhanghai.compose.preference.SwitchPreference
import me.zhanghai.compose.preference.preferenceTheme
import si.inova.kotlinova.core.activity.requireActivity
import si.inova.kotlinova.navigation.screenkeys.ScreenKey
import si.inova.kotlinova.navigation.screens.InjectNavigationScreen
import si.inova.kotlinova.navigation.screens.Screen

@InjectNavigationScreen
class SyncNowScreen : Screen<SyncNowScreenKey>() {
   @Composable
   override fun Content(key: SyncNowScreenKey) {
      val activity = LocalContext.current.requireActivity().let { it as TaskerConfigurationActivity }

      var onlyOnWatchface by rememberSaveable {
         mutableStateOf(
            activity.existingData.getBoolean(BundleKeys.ONLY_ON_WATCHFACE, false)
         )
      }

      SideEffect {
         if (activity.existingData.isEmpty) {
            save(activity, onlyOnWatchface)
         }
      }

      SyncNowScreenContent(
         onlyOnWatchface,
         {
            onlyOnWatchface = it
            save(activity, it)
         },
         {
            activity.finish()
         }
      )
   }

   private fun save(
      activity: TaskerConfigurationActivity,
      onlyOnWatchface: Boolean,
   ) {
      val message = if (onlyOnWatchface) {
         activity.getString(R.string.sync_now_only_on_watchface)
      } else {
         activity.getString(R.string.sync_now)
      }

      activity.saveConfiguration(
         bundleOf(
            BundleKeys.ACTION to TaskerAction.SYNC_NOW.name,
            BundleKeys.ONLY_ON_WATCHFACE to onlyOnWatchface,
         ),
         message
      )
   }
}

@Composable
private fun SyncNowScreenContent(
   onlyOnWatchface: Boolean,
   setOnlyOnWatchface: (value: Boolean) -> Unit,
   save: () -> Unit,
) {
   CompositionLocalProvider(LocalPreferenceTheme provides preferenceTheme()) {
      Column(
         verticalArrangement = Arrangement.spacedBy(16.dp),
         modifier = Modifier.safeDrawingPadding(),
         horizontalAlignment = Alignment.CenterHorizontally
      ) {
         SwitchPreference(
            onlyOnWatchface,
            { setOnlyOnWatchface(it) },
            title = { Text(stringResource(R.string.only_on_watchface_title)) },
            summary = { Text(stringResource(R.string.only_on_watchface_description)) }
         )

         Button(onClick = save, modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(stringResource(R.string.save))
         }
      }
   }
}

@FullScreenPreviews
@Composable
@ShowkaseComposable(group = "test")
internal fun SyncNowScreenPreview() {
   PreviewTheme {
      SyncNowScreenContent(true, {}, {})
   }
}

@Parcelize
data object SyncNowScreenKey : ScreenKey()
