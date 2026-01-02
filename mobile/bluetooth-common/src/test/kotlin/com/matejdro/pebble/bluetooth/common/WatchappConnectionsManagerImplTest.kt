package com.matejdro.pebble.bluetooth.common

import dispatch.core.DefaultCoroutineScope
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.rebble.pebblekit2.common.model.PebbleDictionary
import io.rebble.pebblekit2.common.model.PebbleDictionaryItem
import io.rebble.pebblekit2.common.model.ReceiveResult
import io.rebble.pebblekit2.common.model.WatchIdentifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import si.inova.kotlinova.core.test.TestScopeWithDispatcherProvider
import si.inova.kotlinova.core.test.outcomes.ThrowingErrorReporter
import java.util.UUID

class WatchappConnectionsManagerImplTest {
   private val scope = TestScopeWithDispatcherProvider()
   private val errorReporter = ThrowingErrorReporter(scope)
      .apply { reportToTestScope = false }

   private val createdConnections = HashMap<String, TestWatchappConnection>()

   private val connectionsManager = WatchappConnectionsManagerImpl(
      DefaultCoroutineScope(scope.backgroundScope.coroutineContext),
      ::TestWatchappConnection,
      errorReporter,
      WATCHAPP_UUID
   )

   @Test
   fun `Instantiate connections on app start`() = scope.runTest {
      connectionsManager.onAppOpened(WATCHAPP_UUID, WatchIdentifier("Watch1"))
      connectionsManager.onAppOpened(WATCHAPP_UUID, WatchIdentifier("Watch2"))
      runCurrent()

      createdConnections.keys.toSet().shouldContainExactly("Watch1", "Watch2")
      createdConnections.getValue("Watch1").watchIdentifier shouldBe WatchIdentifier("Watch1")
      createdConnections.getValue("Watch2").watchIdentifier shouldBe WatchIdentifier("Watch2")
   }

   @Test
   fun `Ignore connections from invalid watchapps`() = scope.runTest {
      connectionsManager.onAppOpened(INVALID_APP_UUID, WatchIdentifier("Watch1"))
      runCurrent()

      createdConnections.keys.shouldBeEmpty()
   }

   @Test
   fun `Cancel connections on app close`() = scope.runTest {
      connectionsManager.onAppOpened(WATCHAPP_UUID, WatchIdentifier("Watch1"))
      connectionsManager.onAppOpened(WATCHAPP_UUID, WatchIdentifier("Watch2"))
      runCurrent()

      connectionsManager.onAppClosed(WATCHAPP_UUID, WatchIdentifier("Watch2"))
      runCurrent()

      createdConnections.getValue("Watch1").coroutineScope.coroutineContext.job.isActive shouldBe true
      createdConnections.getValue("Watch2").coroutineScope.coroutineContext.job.isActive shouldBe false
   }

   @Test
   fun `Forward Appmessages`() = scope.runTest {
      connectionsManager.onAppOpened(WATCHAPP_UUID, WatchIdentifier("Watch1"))
      connectionsManager.onAppOpened(WATCHAPP_UUID, WatchIdentifier("Watch2"))
      connectionsManager.onMessageReceived(
         WATCHAPP_UUID,
         mapOf(1u to PebbleDictionaryItem.UInt32(1u)),
         WatchIdentifier("Watch1")
      )
      connectionsManager.onMessageReceived(
         WATCHAPP_UUID,
         mapOf(1u to PebbleDictionaryItem.UInt32(2u)),
         WatchIdentifier("Watch2")
      )
      runCurrent()

      createdConnections.getValue("Watch1").receivedPackets.shouldContainExactly(
         mapOf(1u to PebbleDictionaryItem.UInt32(1u))
      )
      createdConnections.getValue("Watch2").receivedPackets.shouldContainExactly(
         mapOf(1u to PebbleDictionaryItem.UInt32(2u))
      )
   }

   @Test
   fun `Forward ack statuses`() = scope.runTest {
      connectionsManager.onAppOpened(WATCHAPP_UUID, WatchIdentifier("Watch1"))
      connectionsManager.onAppOpened(WATCHAPP_UUID, WatchIdentifier("Watch2"))
      runCurrent()

      createdConnections.getValue("Watch1").returnResult = ReceiveResult.Ack
      createdConnections.getValue("Watch2").returnResult = ReceiveResult.Nack

      connectionsManager.onMessageReceived(
         WATCHAPP_UUID,
         mapOf(1u to PebbleDictionaryItem.UInt32(1u)),
         WatchIdentifier("Watch1")
      ) shouldBe ReceiveResult.Ack

      connectionsManager.onMessageReceived(
         WATCHAPP_UUID,
         mapOf(1u to PebbleDictionaryItem.UInt32(2u)),
         WatchIdentifier("Watch2")
      ) shouldBe ReceiveResult.Nack
   }

   @Test
   fun `Forward connection crashes to error reporter`() = scope.runTest {
      connectionsManager.onAppOpened(WATCHAPP_UUID, WatchIdentifier("Watch1"))
      runCurrent()

      val exception = Exception("Crash")

      createdConnections.getValue("Watch1").coroutineScope.launch {
         throw exception
      }
      runCurrent()

      errorReporter.receivedExceptions.shouldHaveSize(1).first().shouldBeSameInstanceAs(exception)
   }

   @Test
   fun `Forward message received crashes to error reporter`() = scope.runTest {
      connectionsManager.onAppOpened(WATCHAPP_UUID, WatchIdentifier("Watch1"))
      runCurrent()

      val exception = Exception("Crash")
      createdConnections.getValue("Watch1").crashOnPacketReceived = exception

      connectionsManager.onMessageReceived(
         WATCHAPP_UUID,
         mapOf(1u to PebbleDictionaryItem.UInt32(2u)),
         WatchIdentifier("Watch1")
      ) shouldBe ReceiveResult.Nack

      errorReporter.receivedExceptions.shouldHaveSize(1).first().shouldBeSameInstanceAs(exception)
   }

   private inner class TestWatchappConnection(
      val watchIdentifier: WatchIdentifier,
      val coroutineScope: CoroutineScope,
   ) : WatchAppConnection {
      val receivedPackets = ArrayList<PebbleDictionary>()
      var returnResult: ReceiveResult = ReceiveResult.Ack
      var crashOnPacketReceived: Throwable? = null

      init {
         createdConnections[watchIdentifier.value] = this
      }

      override suspend fun onPacketReceived(data: PebbleDictionary): ReceiveResult {
         crashOnPacketReceived?.let { throw it }
         receivedPackets += data
         return returnResult
      }
   }
}

private val INVALID_APP_UUID = UUID.fromString("a4020bb5-6f9c-4a08-9606-d676d99e842a")
private val WATCHAPP_UUID: UUID = UUID.fromString("54be2d78-a70c-4573-a73e-5b0f1323d4cd")
