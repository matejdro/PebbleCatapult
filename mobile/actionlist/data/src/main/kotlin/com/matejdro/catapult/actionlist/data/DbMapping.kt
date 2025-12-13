package com.matejdro.catapult.actionlist.data

import com.matejdro.catapult.actionlist.api.CatapultAction
import com.matejdro.catapult.actionlist.api.CatapultDirectory
import com.matejdro.catapult.actionlist.sqldelight.generated.DbDirectory
import com.matejdro.catapult.actionlist.sqldelight.generated.SelectAll
import com.matejdro.catapult.actionlist.sqldelight.generated.SelectSingle

fun DbDirectory.toDirectory(): CatapultDirectory {
   return CatapultDirectory(id.toInt(), title)
}

fun CatapultDirectory.toDb(): DbDirectory {
   return DbDirectory(id.toLong(), title)
}

fun SelectAll.toCatapultAction(): CatapultAction {
   return CatapultAction(title, directoryId.toInt(), id.toInt(), taskerTaskName, targetDirectoryId?.toInt(), targetDirectoryName)
}

fun SelectSingle.toCatapultAction(): CatapultAction {
   return CatapultAction(title, directoryId.toInt(), id.toInt(), taskerTaskName, targetDirectoryId?.toInt(), targetDirectoryName)
}
