package com.matejdro.catapult.tasker

import android.os.Bundle
import com.matejdro.catapult.actionlist.api.CatapultActionRepository
import com.matejdro.catapult.bluetooth.WatchappOpenController
import com.matejdro.catapult.bluetooth.api.WATCHAPP_UUID
import dev.zacsweers.metro.Inject
import dispatch.core.withDefault
import io.rebble.pebblekit2.client.PebbleInfoRetriever
import io.rebble.pebblekit2.client.PebbleSender
import io.rebble.pebblekit2.model.Watchapp
import kotlinx.coroutines.flow.first
import logcat.logcat

@Inject
class TaskerActionRunner(
   private val actionRepository: CatapultActionRepository,
   private val sender: PebbleSender,
   private val pebbleInfoRetriever: PebbleInfoRetriever,
   private val openController: WatchappOpenController,
) {
   suspend fun run(bundle: Bundle) {
      val actionName = bundle.getString(BundleKeys.ACTION) ?: error("Missing action from bundle")
      val action = enumValueOf<TaskerAction>(actionName)

      when (action) {
         TaskerAction.TOGGLE_ACTIONS -> runToggleAction(bundle)

         TaskerAction.SYNC_NOW -> runSyncAction(bundle)
      }
   }

   private suspend fun runToggleAction(bundle: Bundle) {
      val directoryId = bundle.getInt(BundleKeys.DIRECTORY_ID, 1)
      val actionsToEnable = bundle.getString(BundleKeys.ENABLED_TASK_IDS)
         ?.split(",")?.mapNotNull { it.toIntOrNull() }.orEmpty()
      val actionsToDisable = bundle.getString(BundleKeys.DISABLED_TASK_IDS)
         ?.split(",")?.mapNotNull { it.toIntOrNull() }.orEmpty()

      actionRepository.massToggle(directoryId, actionsToEnable, actionsToDisable)
   }

   private suspend fun runSyncAction(bundle: Bundle) {
      val onlyOnWatchface = bundle.getBoolean(BundleKeys.ONLY_ON_WATCHFACE, false)
      logcat { "Syncnow, onlyOnWatchface: $onlyOnWatchface" }
      if (onlyOnWatchface) {
         withDefault {
            val connectedWatches = pebbleInfoRetriever.getConnectedWatches().first()
            logcat { "Connected watches: ${connectedWatches.map { it.id to it.name }}" }

            val watchesOnWatchface = connectedWatches.filter { watch ->
               val runningApp = pebbleInfoRetriever.getActiveApp(watch.id).first()
               logcat { "Running app on ${watch.id}: ${runningApp ?: "null"}" }

               runningApp?.type == Watchapp.Type.WATCHFACE
            }

            openController.setNextWatchappOpenForAutoSync()
            sender.startAppOnTheWatch(WATCHAPP_UUID, watchesOnWatchface.map { it.id })
         }
      } else {
         openController.setNextWatchappOpenForAutoSync()
         sender.startAppOnTheWatch(WATCHAPP_UUID)
      }
   }
}
