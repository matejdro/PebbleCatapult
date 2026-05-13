package com.matejdro.catapult.tasker

import android.os.Bundle
import com.matejdro.catapult.actionlist.api.CatapultAction
import com.matejdro.catapult.actionlist.test.FakeCatapultActionRepository
import com.matejdro.catapult.bluetooth.FakePebbleInfoRetriever
import com.matejdro.catapult.bluetooth.FakeWatchappOpenController
import com.matejdro.catapult.bluetooth.api.WATCHAPP_UUID
import com.matejdro.pebble.bluetooth.common.test.FakePebbleSender
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.throwable.shouldHaveMessage
import io.rebble.pebblekit2.common.model.TimelineLayout
import io.rebble.pebblekit2.common.model.TimelineLayoutType
import io.rebble.pebblekit2.common.model.TimelinePin
import io.rebble.pebblekit2.common.model.WatchIdentifier
import io.rebble.pebblekit2.model.Watchapp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import si.inova.kotlinova.core.test.TestScopeWithDispatcherProvider
import si.inova.kotlinova.core.test.outcomes.shouldBeSuccessWithData
import si.inova.kotlinova.core.test.time.virtualTimeProvider
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toKotlinInstant

class TaskerActionRunnerTest {
   private val scope = TestScopeWithDispatcherProvider()
   private val repo = FakeCatapultActionRepository()
   private val pebbleSender = FakePebbleSender(scope.virtualTimeProvider())
   private val pebbleInfoRetriever = FakePebbleInfoRetriever()
   private val openController = FakeWatchappOpenController()
   private val runner = TaskerActionRunner(
      repo,
      pebbleSender,
      pebbleInfoRetriever,
      openController,
      scope.virtualTimeProvider()
   )

   @Test
   fun `Run toggle action`() = scope.runTest {
      repo.insert(
         CatapultAction("Action A", 10, 1, enabled = false),
         CatapultAction("Action B", 10, 2, enabled = false),
         CatapultAction("Action C", 10, 3, enabled = true),
         CatapultAction("Action D", 10, 4, enabled = true),
      )

      runner.run(
         Bundle().apply {
            putString(BundleKeys.ACTION, "TOGGLE_ACTIONS")
            putInt(BundleKeys.DIRECTORY_ID, 10)
            putString(BundleKeys.ENABLED_TASK_IDS, "1,2")
            putString(BundleKeys.DISABLED_TASK_IDS, "3,4")
         }
      )
      runCurrent()

      repo.getAll(10).first() shouldBeSuccessWithData listOf(
         CatapultAction("Action A", 10, 1, enabled = true),
         CatapultAction("Action B", 10, 2, enabled = true),
         CatapultAction("Action C", 10, 3, enabled = false),
         CatapultAction("Action D", 10, 4, enabled = false),
      )
   }

   @Test
   fun `Start app on all watches when running normal sync now`() = scope.runTest {
      runner.run(
         Bundle().apply {
            putString(BundleKeys.ACTION, "SYNC_NOW")
         }
      )
      runCurrent()

      openController.isNextWatchappOpenForAutoSync() shouldBe true

      pebbleSender.startedApps.shouldContainExactly(
         FakePebbleSender.AppLifecycleEvent(WATCHAPP_UUID, null)
      )
   }

   @Test
   fun `Start app only on watches that are on the watchfaces with the only watchface flag`() = scope.runTest {
      pebbleInfoRetriever.setConnectedWatchIds(
         listOf(
            WatchIdentifier("1"),
            WatchIdentifier("2"),
            WatchIdentifier("3"),
         )
      )

      pebbleInfoRetriever.setActiveApp(
         WatchIdentifier("1"),
         Watchapp(UUID(1, 1), "App", Watchapp.Type.WATCHAPP)
      )

      pebbleInfoRetriever.setActiveApp(
         WatchIdentifier("2"),
         null
      )

      pebbleInfoRetriever.setActiveApp(
         WatchIdentifier("3"),
         Watchapp(UUID(1, 2), "Watchface", Watchapp.Type.WATCHFACE)

      )

      runner.run(
         Bundle().apply {
            putString(BundleKeys.ACTION, "SYNC_NOW")
            putBoolean(BundleKeys.ONLY_ON_WATCHFACE, true)
         }
      )
      runCurrent()

      openController.isNextWatchappOpenForAutoSync() shouldBe true

      pebbleSender.startedApps.shouldContainExactly(
         FakePebbleSender.AppLifecycleEvent(
            WATCHAPP_UUID,
            listOf(WatchIdentifier("3"))
         ),
      )
   }

   @Test
   fun `Insert pin with full data`() = scope.runTest {
      runner.run(
         Bundle().apply {
            putString(BundleKeys.ACTION, "CREATE_PIN")
            putString(BundleKeys.ID, "10")
            putString(BundleKeys.TITLE, "Title")
            putString(BundleKeys.TEXT, "Text")
            putString(BundleKeys.START_DATE, "2026-02-10")
            putString(BundleKeys.START_TIME, "10:00")
            putString(BundleKeys.DURATION, "4")
            putString(BundleKeys.ICON, "TIMELINE_WEATHER")
         }
      )
      runCurrent()

      val targetInstant = LocalDateTime.of(2026, 2, 10, 10, 0)
         .atZone(ZoneId.of("UTC"))
         .toInstant()
         .toKotlinInstant()

      pebbleSender.insertedPins.shouldContainExactly(
         TimelinePin(
            "10",
            targetInstant,
            4.minutes,
            TimelineLayout(
               TimelineLayoutType.CALENDAR_PIN,
               "Title",
               body = "Text",
               tinyIcon = "system://images/TIMELINE_WEATHER",
            )
         )
      )
   }

   @Test
   fun `Insert pin with minimal data`() = scope.runTest {
      runner.run(
         Bundle().apply {
            putString(BundleKeys.ACTION, "CREATE_PIN")
            putString(BundleKeys.ID, "10")
            putString(BundleKeys.TITLE, "Title")
            putString(BundleKeys.START_DATE, "2026-02-10")
            putString(BundleKeys.START_TIME, "10:00")
         }
      )
      runCurrent()

      val targetInstant = LocalDateTime.of(2026, 2, 10, 10, 0)
         .atZone(ZoneId.of("UTC"))
         .toInstant()
         .toKotlinInstant()

      pebbleSender.insertedPins.shouldContainExactly(
         TimelinePin(
            "10",
            targetInstant,
            layout = TimelineLayout(
               TimelineLayoutType.GENERIC_PIN,
               "Title",
            )
         )
      )
   }

   @Test
   fun `Throw exception on invalid formatting`() = scope.runTest {
      shouldThrow<TaskerInvalidInputException> {
         runner.run(
            Bundle().apply {
               putString(BundleKeys.ACTION, "CREATE_PIN")
               putString(BundleKeys.ID, "10")
               putString(BundleKeys.TITLE, "Title")
               putString(BundleKeys.START_DATE, "2026-02")
               putString(BundleKeys.START_TIME, "10:00")
            }
         )
         runCurrent()
      }.shouldHaveMessage("Invalid date format: '2026-02'")
   }

   @Test
   fun `Delete pin`() = scope.runTest {
      runner.run(
         Bundle().apply {
            putString(BundleKeys.ACTION, "DELETE_PIN")
            putString(BundleKeys.ID, "10")
         }
      )
      runCurrent()

      pebbleSender.deletedPins.shouldContainExactly("10")
   }
}
