package com.matejdro.catapult.tasklist.data

import com.matejdro.catapult.tasklist.api.CatapultDirectory
import com.matejdro.catapult.tasklist.sqldelight.generated.DbDirectory

fun DbDirectory.toDirectory(): CatapultDirectory {
   return CatapultDirectory(id.toInt(), title)
}

fun CatapultDirectory.toDb(): DbDirectory {
   return DbDirectory(id.toLong(), title)
}
