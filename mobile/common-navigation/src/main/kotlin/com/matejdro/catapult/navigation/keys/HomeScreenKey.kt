package com.matejdro.catapult.navigation.keys

import com.matejdro.catapult.navigation.keys.base.BaseScreenKey
import com.matejdro.catapult.navigation.keys.base.TabContainerKey
import kotlinx.parcelize.Parcelize

@Parcelize
data object HomeScreenKey : BaseScreenKey(), TabContainerKey
