package com.matejdro.catapult.tasker.ui.screens.createpin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.matejdro.catapult.tasker.BundleKeys
import com.matejdro.catapult.tasker.TaskerAction
import com.matejdro.catapult.tasker.TaskerPluginConstants
import com.matejdro.catapult.tasker.ui.R
import com.matejdro.catapult.tasker.ui.TaskerConfigurationActivity
import com.matejdro.catapult.ui.debugging.FullScreenPreviews
import com.matejdro.catapult.ui.debugging.PreviewTheme
import kotlinx.serialization.Serializable
import si.inova.kotlinova.core.activity.requireActivity
import si.inova.kotlinova.navigation.screenkeys.ScreenKey
import si.inova.kotlinova.navigation.screens.InjectNavigationScreen
import si.inova.kotlinova.navigation.screens.Screen

@InjectNavigationScreen
class DeletePinScreen : Screen<DeletePinScreenKey>() {
   @Composable
   override fun Content(key: DeletePinScreenKey) {
      val activity = LocalContext.current.requireActivity().let { it as TaskerConfigurationActivity }

      val id = rememberTextFieldState(activity.existingData.getString(BundleKeys.ID).orEmpty())

      fun save() {
         activity.saveConfiguration(
            bundleOf(
               BundleKeys.ACTION to TaskerAction.DELETE_PIN.name,
               BundleKeys.ID to id.text.toString(),
               TaskerPluginConstants.VARIABLE_REPLACE_KEYS to BundleKeys.ID
            ),
            "Delete pin '${id.text}' from the timeline"
         )
      }

      DeletePinScreenContent(
         id = id,
      )

      Autosave(
         id = id,
         save = { save() }
      )
   }
}

@Composable
private fun DeletePinScreenContent(
   id: TextFieldState,
) {
   Column(
      Modifier
         .verticalScroll(rememberScrollState())
         .padding(8.dp)
         .safeDrawingPadding(),
      verticalArrangement = Arrangement.spacedBy(8.dp)
   ) {
      Text(stringResource(R.string.pin_id), style = MaterialTheme.typography.headlineSmall)
      Text(
         stringResource(R.string.pin_id_description),
         style = MaterialTheme.typography.bodySmall
      )
      TextField(
         id,
         modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
         lineLimits = TextFieldLineLimits.SingleLine,
         keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
      )
   }
}

@Composable
private fun Autosave(
   id: TextFieldState,
   save: () -> Unit,
) {
   // Use all texts to ensure they get recomposed here
   id.text

   SideEffect {
      save()
   }
}

@FullScreenPreviews
@Composable
@ShowkaseComposable(group = "test")
internal fun DeletePinScreenPreview() {
   PreviewTheme {
      DeletePinScreenContent(
         rememberTextFieldState(),
      )
   }
}

@Serializable
data object DeletePinScreenKey : ScreenKey()
