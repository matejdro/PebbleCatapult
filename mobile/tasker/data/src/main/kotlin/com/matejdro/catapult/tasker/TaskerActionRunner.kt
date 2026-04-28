package com.matejdro.catapult.tasker

import android.os.Bundle
import com.matejdro.catapult.actionlist.api.CatapultActionRepository
import com.matejdro.catapult.bluetooth.WatchappOpenController
import com.matejdro.catapult.bluetooth.api.WATCHAPP_UUID
import dev.zacsweers.metro.Inject
import dispatch.core.withDefault
import io.rebble.pebblekit2.client.PebbleInfoRetriever
import io.rebble.pebblekit2.client.PebbleSender
import io.rebble.pebblekit2.common.model.TimelineLayout
import io.rebble.pebblekit2.common.model.TimelineLayoutType
import io.rebble.pebblekit2.common.model.TimelinePin
import io.rebble.pebblekit2.common.model.TimelineResult
import io.rebble.pebblekit2.model.Watchapp
import kotlinx.coroutines.flow.first
import logcat.logcat
import si.inova.kotlinova.core.exceptions.UnknownCauseException
import si.inova.kotlinova.core.time.TimeProvider
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeParseException
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toKotlinInstant

@Inject
class TaskerActionRunner(
   private val actionRepository: CatapultActionRepository,
   private val sender: PebbleSender,
   private val pebbleInfoRetriever: PebbleInfoRetriever,
   private val openController: WatchappOpenController,
   private val timeProvider: TimeProvider,
) {
   suspend fun run(bundle: Bundle) {
      val actionName = bundle.getString(BundleKeys.ACTION) ?: error("Missing action from bundle")
      val action = enumValueOf<TaskerAction>(actionName)

      when (action) {
         TaskerAction.TOGGLE_ACTIONS -> runToggleAction(bundle)

         TaskerAction.SYNC_NOW -> runSyncAction(bundle)
         TaskerAction.CREATE_PIN -> runCreatePin(bundle)
      }
   }

   private suspend fun runToggleAction(bundle: Bundle) {
      val directoryId = bundle.getInt(BundleKeys.DIRECTORY_ID, 1)
      val actionsToEnable = bundle.getString(BundleKeys.ENABLED_TASK_IDS)
         ?.split(",")
         ?.mapNotNull { it.toIntOrNull() }
         .orEmpty()
      val actionsToDisable = bundle.getString(BundleKeys.DISABLED_TASK_IDS)
         ?.split(",")
         ?.mapNotNull { it.toIntOrNull() }
         .orEmpty()

      actionRepository.massToggle(directory = directoryId, enable = actionsToEnable, disable = actionsToDisable)
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

   @Suppress("ThrowsCount") // Input validation
   private suspend fun runCreatePin(bundle: Bundle) {
      val id = bundle.getString(BundleKeys.ID)
      if (id.isNullOrBlank()) {
         throw TaskerInvalidInputException("ID is mandatory")
      }

      val title = bundle.getString(BundleKeys.TITLE)?.takeIf { it.isNotBlank() }
      if (title.isNullOrBlank()) {
         throw TaskerInvalidInputException("Title is mandatory")
      }

      val body = bundle.getString(BundleKeys.TEXT)

      val startDateText = bundle.getString(BundleKeys.START_DATE).orEmpty()
      val startDate = try {
         LocalDate.parse(startDateText)
      } catch (ignored: DateTimeParseException) {
         throw TaskerInvalidInputException("Invalid date format: '$startDateText'")
      }

      val startTimeText = bundle.getString(BundleKeys.START_TIME).orEmpty()
      val startTime = try {
         LocalTime.parse(startTimeText)
      } catch (ignored: DateTimeParseException) {
         throw TaskerInvalidInputException("Invalid time format: '$startTimeText'")
      }

      val duration = bundle.getString(BundleKeys.DURATION)?.takeIf { it.isNotBlank() }?.toIntOrNull()

      val icon = bundle.getString(BundleKeys.ICON)

      val startInstant = startDate.atTime(startTime).atZone(timeProvider.systemDefaultZoneId()).toInstant().toKotlinInstant()

      val result = sender.insertTimelinePin(
         WATCHAPP_UUID,
         TimelinePin(
            id,
            startInstant,
            duration?.minutes,
            TimelineLayout(
               if (duration != null) TimelineLayoutType.CALENDAR_PIN else TimelineLayoutType.GENERIC_PIN,
               title,
               body = body,
               tinyIcon = icon?.let { "system://images/$it" }
            )
         )
      )

      when (result) {
         TimelineResult.FailedNoPebbleApp -> {
            throw TaskerInvalidInputException("Pebble app is not installed")
         }

         TimelineResult.FailedNoPermissions -> {
            throw TaskerInvalidInputException("Catapult watchapp is not installed")
         }

         TimelineResult.FailedUnknownPin -> {
            error("Received unknown pin on insertion. This should never happen")
         }

         TimelineResult.FailedUnsupportedAction -> {
            throw TaskerInvalidInputException("Installed Pebble app is too old for the Timeline feature")
         }

         is TimelineResult.Unknown -> {
            throw UnknownCauseException("Unknown timeline error '${result.message.orEmpty()}'")
         }

         TimelineResult.Success -> {
            // Success! Nothing to do
         }
      }
   }
}
