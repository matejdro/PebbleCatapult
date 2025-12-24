package com.matejdro.catapult.tasker

import android.os.Bundle
import com.matejdro.catapult.actionlist.api.CatapultActionRepository
import dev.zacsweers.metro.Inject

@Inject
class TaskerActionRunner(
   private val actionRepository: CatapultActionRepository,
) {
   suspend fun run(bundle: Bundle) {
      val actionName = bundle.getString(BundleKeys.ACTION) ?: error("Missing action from bundle")
      val action = enumValueOf<TaskerAction>(actionName)

      when (action) {
         TaskerAction.TOGGLE_ACTIONS -> {
            val directoryId = bundle.getInt(BundleKeys.DIRECTORY_ID, 1)
            val actionsToEnable = bundle.getString(BundleKeys.ENABLED_TASK_IDS)
               ?.split(",")?.mapNotNull { it.toIntOrNull() }.orEmpty()
            val actionsToDisable = bundle.getString(BundleKeys.DISABLED_TASK_IDS)
               ?.split(",")?.mapNotNull { it.toIntOrNull() }.orEmpty()

            actionRepository.massToggle(directoryId, actionsToEnable, actionsToDisable)
         }
      }
   }
}
