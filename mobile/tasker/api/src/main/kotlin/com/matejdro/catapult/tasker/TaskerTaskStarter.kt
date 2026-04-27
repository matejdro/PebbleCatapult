package com.matejdro.catapult.tasker

interface TaskerTaskStarter {
   fun startTask(task: String, parameter: String?): Boolean
}
