package com.matejdro.catapult.tasklist.data

import com.matejdro.catapult.tasklist.api.CatapultAction
import com.matejdro.catapult.tasklist.api.CatapultDirectory
import com.matejdro.catapult.tasklist.sqldelight.generated.DbDirectory
import com.matejdro.catapult.tasklist.sqldelight.generated.SelectAll

fun DbDirectory.toDirectory(): CatapultDirectory {
   return CatapultDirectory(id.toInt(), title)
}

fun CatapultDirectory.toDb(): DbDirectory {
   return DbDirectory(id.toLong(), title)
}

fun SelectAll.toCatapultAction(): CatapultAction {
   return CatapultAction(title, directoryId.toInt(), id.toInt(), taskerTaskName, targetDirectoryId?.toInt(), targetDirectoryName)
}
