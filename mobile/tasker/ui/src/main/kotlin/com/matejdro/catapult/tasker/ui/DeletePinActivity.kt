package com.matejdro.catapult.tasker.ui

import com.matejdro.catapult.tasker.ui.screens.createpin.DeletePinScreenKey
import si.inova.kotlinova.navigation.screenkeys.ScreenKey

class DeletePinActivity : TaskerConfigurationActivity() {
   override fun getInitialHistory(): List<ScreenKey> {
      return listOf(DeletePinScreenKey)
   }
}
