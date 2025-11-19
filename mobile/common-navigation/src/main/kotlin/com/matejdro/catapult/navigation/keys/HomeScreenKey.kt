package com.matejdro.catapult.navigation.keys

import com.matejdro.catapult.navigation.keys.base.BaseSingleTopScreenKey
import kotlinx.parcelize.Parcelize
import si.inova.kotlinova.navigation.screenkeys.ScreenKey

@Parcelize
data class HomeScreenKey(val selectedScreen: ScreenKey = FolderListKey) : BaseSingleTopScreenKey()
