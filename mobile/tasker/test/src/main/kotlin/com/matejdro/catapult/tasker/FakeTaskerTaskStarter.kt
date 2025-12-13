package com.matejdro.catapult.tasker

class FakeTaskerTaskStarter : TaskerTaskStarter {
   val startedTasks = mutableListOf<String>()
   var reportStartSuccessful = true

   override fun startTask(task: String): Boolean {
      startedTasks.add(task)

      return reportStartSuccessful
   }
}
