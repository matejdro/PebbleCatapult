package com.matejdro.catapult.navigation.keys

import com.matejdro.catapult.navigation.keys.base.BaseScreenKey
import com.matejdro.catapult.navigation.keys.base.ListKey
import kotlinx.parcelize.Parcelize

@Parcelize
data class DirectoryListKey(
   val targetScreen: String = ActionListKey::class.java.name,
) : BaseScreenKey(), ListKey
