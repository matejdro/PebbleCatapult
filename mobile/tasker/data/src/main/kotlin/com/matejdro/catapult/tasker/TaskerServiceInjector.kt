package com.matejdro.catapult.tasker

interface TaskerServiceInjector {
   fun inject(taskerActionService: TaskerActionService)
   fun inject(legacyTaskerReceiver: LegacyTaskerReceiver)
}
