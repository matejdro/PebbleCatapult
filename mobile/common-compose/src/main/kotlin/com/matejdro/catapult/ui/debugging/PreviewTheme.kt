package com.matejdro.catapult.ui.debugging

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.matejdro.catapult.ui.theme.CatapultTheme
import si.inova.kotlinova.compose.time.ComposeAndroidDateTimeFormatter
import si.inova.kotlinova.compose.time.LocalDateFormatter
import si.inova.kotlinova.core.time.AndroidDateTimeFormatter
import si.inova.kotlinova.core.time.FakeAndroidDateTimeFormatter

@Composable
@Suppress("ModifierMissing") // This is intentional
fun PreviewTheme(
   formatter: AndroidDateTimeFormatter = FakeAndroidDateTimeFormatter(),
   fill: Boolean = true,
   content: @Composable () -> Unit,
) {
   CompositionLocalProvider(
      LocalDateFormatter provides ComposeAndroidDateTimeFormatter(formatter),
   ) {
      // Disable Material You on previews (and screenshot tests) to improve reproducibility
      CatapultTheme(dynamicColor = false) {
         Surface(modifier = if (fill) Modifier.fillMaxSize() else Modifier, content = content)
      }
   }
}
