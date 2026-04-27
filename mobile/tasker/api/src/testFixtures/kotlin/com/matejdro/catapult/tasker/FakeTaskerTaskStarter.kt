package com.matejdro.catapult.tasker

class FakeTaskerTaskStarter : TaskerTaskStarter {
   val startedTasks = mutableListOf<Pair<String, String?>>()
   var reportStartSuccessful = true

   override fun startTask(task: String, parameter: String?): Boolean {
      startedTasks.add(task to parameter)

      return reportStartSuccessful
   }
}
