package com.matejdro.catapult.tasker.ui.screens.actionlist

import kotlinx.parcelize.Parcelize
import si.inova.kotlinova.navigation.screenkeys.ScreenKey

@Parcelize
data class ActionListToggleKey(val directoryId: Int): ScreenKey()
