package com.matejdro.catapult.navigation.keys

import com.matejdro.catapult.navigation.keys.base.DetailKey
import kotlinx.parcelize.Parcelize
import si.inova.kotlinova.navigation.screenkeys.ScreenKey

@Parcelize
data class ActionListKey(val id: Int) : ScreenKey(), DetailKey
