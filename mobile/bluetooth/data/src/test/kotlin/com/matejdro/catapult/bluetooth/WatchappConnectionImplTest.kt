package com.matejdro.catapult.bluetooth

import com.matejdro.bucketsync.FakeBucketSyncRepository
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.rebble.pebblekit2.common.model.PebbleDictionaryItem
import io.rebble.pebblekit2.common.model.ReceiveResult
import io.rebble.pebblekit2.common.model.WatchIdentifier
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import si.inova.kotlinova.core.test.TestScopeWithDispatcherProvider
import si.inova.kotlinova.core.test.time.virtualTimeProvider
import kotlin.time.Duration.Companion.seconds

class WatchappConnectionImplTest {
   private val scope = TestScopeWithDispatcherProvider()

   private val sender = FakePebbleSender(scope.virtualTimeProvider())
   private val bucketSyncRepository = FakeBucketSyncRepository()

   private val connection = WatchappConnectionImpl(
      WatchIdentifier("watch"),
      scope.backgroundScope,
      bucketSyncRepository,
      sender,
   )

   @Test
   fun `Nack unknown packets`() = scope.runTest {
      val result = connection.onPacketReceived(
         mapOf(
            0u to PebbleDictionaryItem.UInt32(255u),
         )
      )
      runCurrent()

      result shouldBe ReceiveResult.Nack
   }

   @Test
   fun `Send only version back when watch packets do not match`() = scope.runTest {
      val result = connection.onPacketReceived(
         mapOf(
            0u to PebbleDictionaryItem.UInt32(0u),
            1u to PebbleDictionaryItem.UInt32(PROTOCOL_VERSION + 1u),
            2u to PebbleDictionaryItem.UInt32(1u),
            3u to PebbleDictionaryItem.UInt32(1000u),
         )
      )
      runCurrent()

      result shouldBe ReceiveResult.Ack

      sender.sentData.shouldContainExactly(
         mapOf(
            0u to PebbleDictionaryItem.UInt8(1u),
            1u to PebbleDictionaryItem.UInt16(PROTOCOL_VERSION),
         )
      )
   }

   @Test
   fun `Only send status 3 when watch is up to date`() = scope.runTest {
      val result = receiveStandardHelloPacket()
      runCurrent()

      result shouldBe ReceiveResult.Ack

      sender.sentData.shouldContainExactly(
         mapOf(
            0u to PebbleDictionaryItem.UInt8(1u),
            1u to PebbleDictionaryItem.UInt16(PROTOCOL_VERSION),
            2u to PebbleDictionaryItem.ByteArray(
               byteArrayOf(
                  2
               )
            ),
         )
      )
   }

   @Test
   fun `Send list of updated buckets in a single packet`() = scope.runTest {
      bucketSyncRepository.updateBucket(1u, byteArrayOf(1))
      bucketSyncRepository.updateBucket(2u, byteArrayOf(2))

      val result = receiveStandardHelloPacket(bufferSize = 53u)
      runCurrent()

      result shouldBe ReceiveResult.Ack

      sender.sentData.shouldContainExactly(
         mapOf(
            0u to PebbleDictionaryItem.UInt8(1u),
            1u to PebbleDictionaryItem.UInt16(PROTOCOL_VERSION),
            2u to PebbleDictionaryItem.ByteArray(
               byteArrayOf(
                  1, // Status
                  0, 2, // Latest version
                  2, // Num of active buckets
                  1, 0, // Metadata for bucket 1
                  2, 0, // Metadata for bucket 2
                  1, 1, 1, // Sync data for bucket 1
                  2, 1, 2, // Sync data for bucket 2
               )
            ),
         )
      )
   }

   @Test
   fun `Send list of updated buckets in two packets`() = scope.runTest {
      bucketSyncRepository.updateBucket(1u, byteArrayOf(1))
      bucketSyncRepository.updateBucket(2u, byteArrayOf(2))

      val result = receiveStandardHelloPacket(bufferSize = 52u)
      runCurrent()

      result shouldBe ReceiveResult.Ack

      sender.sentData.shouldContainExactly(
         mapOf(
            0u to PebbleDictionaryItem.UInt8(1u),
            1u to PebbleDictionaryItem.UInt16(PROTOCOL_VERSION),
            2u to PebbleDictionaryItem.ByteArray(
               byteArrayOf(
                  0, // Status
                  0, 2, // Latest version
                  2, // Num of active buckets
                  1, 0, // Metadata for bucket 1
                  2, 0, // Metadata for bucket 2
                  1, 1, 1, // Sync data for bucket 1
               )
            ),
         ),
         mapOf(
            0u to PebbleDictionaryItem.UInt8(3u),
            1u to PebbleDictionaryItem.ByteArray(
               byteArrayOf(
                  1, // Status
                  2, 1, 2, // Sync data for bucket 2
               )
            ),
         )
      )
   }

   @Test
   fun `Send list of updated buckets in three packets`() = scope.runTest {
      bucketSyncRepository.updateBucket(1u, byteArrayOf(1))
      bucketSyncRepository.updateBucket(2u, byteArrayOf(2))
      bucketSyncRepository.updateBucket(3u, ByteArray(33) { 3 })

      val result = receiveStandardHelloPacket(bufferSize = 52u)
      runCurrent()

      result shouldBe ReceiveResult.Ack

      sender.sentData.shouldContainExactly(
         mapOf(
            0u to PebbleDictionaryItem.UInt8(1u),
            1u to PebbleDictionaryItem.UInt16(PROTOCOL_VERSION),
            2u to PebbleDictionaryItem.ByteArray(
               byteArrayOf(
                  0, // Status
                  0, 3, // Latest version
                  3, // Num of active buckets
                  1, 0, // Metadata for bucket 1
                  2, 0, // Metadata for bucket 2
                  3, 0, // Metadata for bucket 3
                  1, 1, 1, // Sync data for bucket 1
               )
            ),
         ),
         mapOf(
            0u to PebbleDictionaryItem.UInt8(3u),
            1u to PebbleDictionaryItem.ByteArray(
               byteArrayOf(
                  0, // Status
                  2, 1, 2, // Sync data for bucket 2
               )
            ),
         ),
         mapOf(
            0u to PebbleDictionaryItem.UInt8(3u),
            1u to PebbleDictionaryItem.ByteArray(
               byteArrayOf(
                  1, // Status
                  3, 33 // Sync data for bucket 3
               ) + ByteArray(33) { 3 }
            ),
         )
      )
   }

   @Test
   fun `Send bucketsync data after Acking first packet`() = scope.runTest {
      sender.pauseSending = true

      val result = async { receiveStandardHelloPacket() }
      runCurrent()

      result.getCompleted() shouldBe ReceiveResult.Ack
   }

   @Test
   fun `Send new sync packet if buckets update after initial sync packet `() = scope.runTest {
      receiveStandardHelloPacket(bufferSize = 52u)
      runCurrent()

      sender.sentPackets.clear()

      bucketSyncRepository.updateBucket(1u, byteArrayOf(1))
      bucketSyncRepository.updateBucket(2u, byteArrayOf(2))
      delay(1.seconds)

      sender.sentData.shouldContainExactly(
         mapOf(
            0u to PebbleDictionaryItem.UInt8(2u),
            1u to PebbleDictionaryItem.ByteArray(
               byteArrayOf(
                  1, // Status
                  0, 2, // Latest version
                  2, // Num of active buckets
                  1, 0, // Metadata for bucket 1
                  2, 0, // Metadata for bucket 2
                  1, 1, 1, // Sync data for bucket 1
                  2, 1, 2, // Sync data for bucket 2
               )
            ),
         )
      )
   }

   @Test
   fun `Send new sync packet if large size buckets update after initial sync packet in 3 packets`() = scope.runTest {
      receiveStandardHelloPacket(bufferSize = 52u)
      runCurrent()

      sender.sentPackets.clear()

      bucketSyncRepository.updateBucket(1u, byteArrayOf(1))
      bucketSyncRepository.updateBucket(2u, ByteArray(33) { 2 })
      bucketSyncRepository.updateBucket(3u, ByteArray(33) { 3 })
      delay(1.seconds)

      sender.sentData.shouldContainExactly(
         mapOf(
            0u to PebbleDictionaryItem.UInt8(2u),
            1u to PebbleDictionaryItem.ByteArray(
               byteArrayOf(
                  0, // Status
                  0, 3, // Latest version
                  3, // Num of active buckets
                  1, 0, // Metadata for bucket 1
                  2, 0, // Metadata for bucket 2
                  3, 0, // Metadata for bucket 2
                  1, 1, 1, // Sync data for bucket 1
               )
            ),
         ),
         mapOf(
            0u to PebbleDictionaryItem.UInt8(3u),
            1u to PebbleDictionaryItem.ByteArray(
               byteArrayOf(
                  0, // Status
                  2, 33, // Sync data for bucket 2
               ) + ByteArray(33) { 2 }
            ),
         ),
         mapOf(
            0u to PebbleDictionaryItem.UInt8(3u),
            1u to PebbleDictionaryItem.ByteArray(
               byteArrayOf(
                  1, // Status
                  3, 33 // Sync data for bucket 3
               ) + ByteArray(33) { 3 }
            ),
         ),
      )
   }

   @Test
   fun `Disallow watch initing twice`() = scope.runTest {
      connection.onPacketReceived(
         mapOf(
            0u to PebbleDictionaryItem.UInt32(255u),
         )
      )

      val result = connection.onPacketReceived(
         mapOf(
            0u to PebbleDictionaryItem.UInt32(255u),
         )
      )
      runCurrent()

      result shouldBe ReceiveResult.Nack
   }

   private suspend fun receiveStandardHelloPacket(version: UInt = 0u, bufferSize: UInt = 1000u): ReceiveResult =
      connection.onPacketReceived(
         mapOf(
            0u to PebbleDictionaryItem.UInt32(0u),
            1u to PebbleDictionaryItem.UInt32(PROTOCOL_VERSION.toUInt()),
            2u to PebbleDictionaryItem.UInt32(version),
            3u to PebbleDictionaryItem.UInt32(bufferSize),
         )
      )
}
