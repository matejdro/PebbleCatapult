package com.matejdro.catapult.tasker.ui

import com.matejdro.catapult.tasker.ui.screens.createpin.CreatePinScreenKey
import si.inova.kotlinova.navigation.screenkeys.ScreenKey

class CreatePinActivity : TaskerConfigurationActivity() {
   override fun getInitialHistory(): List<ScreenKey> {
      return listOf(CreatePinScreenKey)
   }
}
