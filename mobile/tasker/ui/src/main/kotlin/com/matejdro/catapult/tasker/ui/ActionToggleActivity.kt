package com.matejdro.catapult.tasker.ui

import com.matejdro.catapult.navigation.keys.ActionListToggleKey
import com.matejdro.catapult.navigation.keys.DirectoryListKey
import com.matejdro.catapult.tasker.BundleKeys
import si.inova.kotlinova.navigation.screenkeys.ScreenKey

class ActionToggleActivity : TaskerConfigurationActivity() {
   override fun getInitialHistory(): List<ScreenKey> {
      val existingDirectory = existingData.getInt(BundleKeys.DIRECTORY_ID, -1)
      return listOf(
         if (existingDirectory >= 0) {
            ActionListToggleKey(existingDirectory)
         } else {
            DirectoryListKey(ActionListToggleKey::class.java.name)
         }
      )
   }
}
