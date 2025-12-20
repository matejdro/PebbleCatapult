package com.matejdro.catapult.actionlist.data

import com.matejdro.catapult.actionlist.api.CatapultAction
import com.matejdro.catapult.actionlist.api.CatapultDirectory
import com.matejdro.catapult.actionlist.sqldelight.generated.DbDirectory
import com.matejdro.catapult.actionlist.sqldelight.generated.SelectAll
import com.matejdro.catapult.actionlist.sqldelight.generated.SelectSingle

internal fun DbDirectory.toDirectory(): CatapultDirectory {
   return CatapultDirectory(id.toInt(), title)
}

internal fun CatapultDirectory.toDb(): DbDirectory {
   return DbDirectory(id.toLong(), title)
}

internal fun SelectAll.toCatapultAction(): CatapultAction {
   return CatapultAction(
      title,
      directoryId.toInt(),
      id.toInt(),
      taskerTaskName,
      targetDirectoryId?.toInt(),
      targetDirectoryName,
      enabled == 1L
   )
}

internal fun SelectSingle.toCatapultAction(): CatapultAction {
   return CatapultAction(
      title,
      directoryId.toInt(),
      id.toInt(),
      taskerTaskName,
      targetDirectoryId?.toInt(),
      targetDirectoryName,
      enabled == 1L
   )
}
