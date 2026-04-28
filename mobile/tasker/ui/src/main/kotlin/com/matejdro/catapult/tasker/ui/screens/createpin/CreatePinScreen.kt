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
class CreatePinScreen : Screen<CreatePinScreenKey>() {
   @Composable
   override fun Content(key: CreatePinScreenKey) {
      val activity = LocalContext.current.requireActivity().let { it as TaskerConfigurationActivity }

      val id = rememberTextFieldState(activity.existingData.getString(BundleKeys.ID).orEmpty())
      val title = rememberTextFieldState(activity.existingData.getString(BundleKeys.TITLE).orEmpty())
      val text = rememberTextFieldState(activity.existingData.getString(BundleKeys.TEXT).orEmpty())
      val startDate = rememberTextFieldState(activity.existingData.getString(BundleKeys.START_DATE).orEmpty())
      val startTime = rememberTextFieldState(activity.existingData.getString(BundleKeys.START_TIME).orEmpty())
      val duration = rememberTextFieldState(activity.existingData.getString(BundleKeys.DURATION).orEmpty())
      val icon = rememberTextFieldState(activity.existingData.getString(BundleKeys.ICON).orEmpty())

      fun save() {
         activity.saveConfiguration(
            bundleOf(
               BundleKeys.ACTION to TaskerAction.CREATE_PIN.name,
               BundleKeys.ID to id.text.toString(),
               BundleKeys.TITLE to title.text.toString(),
               BundleKeys.START_DATE to startDate.text.toString(),
               BundleKeys.START_TIME to startTime.text.toString(),
               BundleKeys.DURATION to duration.text.toString(),
               BundleKeys.ICON to icon.text.toString(),
               TaskerPluginConstants.VARIABLE_REPLACE_KEYS to listOf(
                  BundleKeys.ID,
                  BundleKeys.TITLE,
                  BundleKeys.START_DATE,
                  BundleKeys.START_TIME,
                  BundleKeys.DURATION,
                  BundleKeys.ICON,
               ).joinToString(" ")
            ),
            activity.getString(R.string.create_pin_description, title.text)
         )
      }

      CreatePinScreenContent(
         id = id,
         title = title,
         text = text,
         startDate = startDate,
         startTime = startTime,
         duration = duration,
         icon = icon,
      )

      Autosave(
         id = id,
         title = title,
         text = text,
         startDate = startDate,
         startTime = startTime,
         duration = duration,
         icon = icon,
         save = { save() },
      )
   }
}

@Composable
private fun CreatePinScreenContent(
   id: TextFieldState,
   title: TextFieldState,
   text: TextFieldState,
   startDate: TextFieldState,
   startTime: TextFieldState,
   duration: TextFieldState,
   icon: TextFieldState,
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

      Text(stringResource(R.string.pin_title), style = MaterialTheme.typography.headlineSmall)
      TextField(
         title,
         modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
         lineLimits = TextFieldLineLimits.SingleLine,
         keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
      )

      Text(stringResource(R.string.pin_text), style = MaterialTheme.typography.headlineSmall)
      Text(stringResource(R.string.pin_text_description), style = MaterialTheme.typography.bodySmall)
      TextField(
         text,
         modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
         lineLimits = TextFieldLineLimits.SingleLine,
         keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
      )

      Text(stringResource(R.string.pin_date), style = MaterialTheme.typography.headlineSmall)
      Text(
         stringResource(R.string.pin_date_description),
         style = MaterialTheme.typography.bodySmall
      )
      TextField(
         startDate,
         modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
         lineLimits = TextFieldLineLimits.SingleLine,
         keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
      )

      Text(stringResource(R.string.pin_time), style = MaterialTheme.typography.headlineSmall)
      Text(
         stringResource(R.string.pin_time_description),
         style = MaterialTheme.typography.bodySmall
      )
      TextField(
         startTime,
         modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
         lineLimits = TextFieldLineLimits.SingleLine,
         keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
      )

      Text(stringResource(R.string.pin_duration), style = MaterialTheme.typography.headlineSmall)
      Text(stringResource(R.string.pin_duration_description), style = MaterialTheme.typography.bodySmall)
      TextField(
         duration,
         modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
         lineLimits = TextFieldLineLimits.SingleLine,
         keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
      )

      Text(stringResource(R.string.pin_icon), style = MaterialTheme.typography.headlineSmall)
      Text(stringResource(R.string.pin_icon_description), style = MaterialTheme.typography.bodySmall)
      TextField(
         icon,
         modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
         lineLimits = TextFieldLineLimits.SingleLine,
         keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
      )
   }
}

@Composable
private fun Autosave(
   id: TextFieldState,
   title: TextFieldState,
   text: TextFieldState,
   startDate: TextFieldState,
   startTime: TextFieldState,
   duration: TextFieldState,
   icon: TextFieldState,
   save: () -> Unit,
) {
   // Use all texts to ensure they get recomposed here
   id.text
   title.text
   text.text
   startDate.text
   startTime.text
   duration.text
   icon.text

   SideEffect {
      save()
   }
}

@FullScreenPreviews
@Composable
@ShowkaseComposable(group = "test")
internal fun CreatePinScreenPreview() {
   PreviewTheme {
      CreatePinScreenContent(
         rememberTextFieldState(),
         rememberTextFieldState(),
         rememberTextFieldState(),
         rememberTextFieldState(),
         rememberTextFieldState(),
         rememberTextFieldState(),
         rememberTextFieldState()
      )
   }
}

@Serializable
data object CreatePinScreenKey : ScreenKey()
