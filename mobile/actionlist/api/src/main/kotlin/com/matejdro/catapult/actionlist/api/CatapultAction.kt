package com.matejdro.catapult.actionlist.api

data class CatapultAction(
   public val title: String,
   public val directoryId: Int,
   public val id: Int = 0,
   public val taskerTaskName: String? = null,
   public val targetDirectoryId: Int? = null,
   public val targetDirectoryName: String? = null,
)
