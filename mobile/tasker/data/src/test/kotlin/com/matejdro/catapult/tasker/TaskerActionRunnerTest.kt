package com.matejdro.catapult.tasker

import androidx.core.os.bundleOf
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
         bundleOf(
            BundleKeys.ACTION to "TOGGLE_ACTIONS",
            BundleKeys.DIRECTORY_ID to 10,
            BundleKeys.ENABLED_TASK_IDS to "1,2",
            BundleKeys.DISABLED_TASK_IDS to "3,4"
         )
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
         bundleOf(
            BundleKeys.ACTION to "SYNC_NOW",
         )
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
         bundleOf(
            BundleKeys.ACTION to "SYNC_NOW",
            BundleKeys.ONLY_ON_WATCHFACE to true,
         )
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
         bundleOf(
            BundleKeys.ACTION to "CREATE_PIN",
            BundleKeys.ID to "10",
            BundleKeys.TITLE to "Title",
            BundleKeys.TEXT to "Text",
            BundleKeys.START_DATE to "2026-02-10",
            BundleKeys.START_TIME to "10:00",
            BundleKeys.DURATION to "4",
            BundleKeys.ICON to "TIMELINE_WEATHER",
         )
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
         bundleOf(
            BundleKeys.ACTION to "CREATE_PIN",
            BundleKeys.ID to "10",
            BundleKeys.TITLE to "Title",
            BundleKeys.START_DATE to "2026-02-10",
            BundleKeys.START_TIME to "10:00",
         )
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
            bundleOf(
               BundleKeys.ACTION to "CREATE_PIN",
               BundleKeys.ID to "10",
               BundleKeys.TITLE to "Title",
               BundleKeys.START_DATE to "2026-02",
               BundleKeys.START_TIME to "10:00",
            )
         )
         runCurrent()
      }.shouldHaveMessage("Invalid date format: '2026-02'")
   }
}
