package com.matejdro.catapult.navigation.keys

import com.matejdro.catapult.navigation.keys.base.BaseScreenKey
import com.matejdro.catapult.navigation.keys.base.TabContainerKey
import kotlinx.serialization.Serializable

@Serializable
data object HomeScreenKey : BaseScreenKey(), TabContainerKey
