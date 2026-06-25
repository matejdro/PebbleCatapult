package com.matejdro.catapult.actionlist.api

import androidx.compose.runtime.Immutable

@Immutable
data class CatapultAction(
   val title: String,
   val directoryId: Int,
   val id: Int = 0,
   val taskerTaskName: String? = null,
   val targetDirectoryId: Int? = null,
   val targetDirectoryName: String? = null,
   val voiceArgument: Boolean = false,
   val enabled: Boolean = true,
   val doNotClose: Boolean = false,
)
