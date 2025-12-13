package com.matejdro.catapult.bluetooth

import com.matejdro.bucketsync.api.Bucket
import com.matejdro.bucketsync.api.BucketUpdate
import com.matejdro.catapult.bluetooth.util.writeUByte
import com.matejdro.catapult.bluetooth.util.writeUShort
import io.rebble.pebblekit2.common.model.PebbleDictionary
import io.rebble.pebblekit2.common.model.PebbleDictionaryItem
import okio.Buffer
import si.inova.kotlinova.core.logging.logcat

/**
 * Write bucketsync data to the [firstPacketBuffer] and, optionally, return additional packets if the first packet
 * is not large enough
 */
internal fun createBucketsyncPackets(
   update: BucketUpdate,
   firstPacketBuffer: Buffer,
   firstPacketLeftoverSize: Int,
   watchBufferSize: Int,
): List<PebbleDictionary> {
   val bucketsToUpdate = createFirstPacket(update, firstPacketLeftoverSize, firstPacketBuffer)

   val additionalPackets = createAdditionalPackets(bucketsToUpdate, watchBufferSize)

   return additionalPackets
}

private fun createAdditionalPackets(
   initialBucketsToUpdate: List<Bucket>,
   watchBufferSize: Int,
): ArrayList<PebbleDictionary> {
   val additionalPackets = ArrayList<PebbleDictionary>()

   var bucketsToUpdate = initialBucketsToUpdate
   while (bucketsToUpdate.isNotEmpty()) {
      var bucketsToSendInThisPacket = 0

      var dataLeftForBuckets = watchBufferSize - SIZE_OF_STATIC_PART_OF_FOLLOW_UP_SYNC_PACKET
      val nextPacketBuffer = Buffer()

      for (updatedBucket in bucketsToUpdate) {
         val sizeToSend = 2 + updatedBucket.data.size
         logcat("AdditionalPackets") { "Size to send $sizeToSend $dataLeftForBuckets" }
         if (sizeToSend <= dataLeftForBuckets) {
            dataLeftForBuckets -= sizeToSend
            bucketsToSendInThisPacket++
         } else {
            break
         }
      }

      nextPacketBuffer.writeUByte(
         if (bucketsToSendInThisPacket == bucketsToUpdate.size) {
            SYNC_STATUS_LAST_PACKET
         } else {
            SYNC_STATUS_MORE_PACKETS
         }
      )

      for (updatedBucket in bucketsToUpdate.take(bucketsToSendInThisPacket)) {
         nextPacketBuffer.writeUByte(updatedBucket.id)
         nextPacketBuffer.writeUByte(updatedBucket.data.size.toUByte())
         nextPacketBuffer.write(updatedBucket.data)
      }

      additionalPackets.add(
         mapOf(
            0u to PebbleDictionaryItem.UInt8(3u),
            1u to PebbleDictionaryItem.ByteArray(nextPacketBuffer.readByteArray()),
         ),
      )

      bucketsToUpdate = bucketsToUpdate.drop(bucketsToSendInThisPacket)
   }

   return additionalPackets
}

private fun createFirstPacket(
   update: BucketUpdate,
   firstPacketLeftoverSize: Int,
   firstPacketBuffer: Buffer,
): List<Bucket> {
   val bucketsToUpdate = update.bucketsToUpdate
   val bucketsToSendInFirstPacket = bucketsToUpdate.calculateNumUpdatesThatFitInto(firstPacketLeftoverSize)

   firstPacketBuffer.writeUByte(
      if (bucketsToSendInFirstPacket == bucketsToUpdate.size) {
         SYNC_STATUS_LAST_PACKET
      } else {
         SYNC_STATUS_MORE_PACKETS
      }
   )

   firstPacketBuffer.writeUShort(update.toVersion)
   firstPacketBuffer.writeUByte(update.activeBuckets.size.toUByte())
   for (activeBucket in update.activeBuckets) {
      firstPacketBuffer.writeUByte(activeBucket.toUByte())
      firstPacketBuffer.writeUByte(0u)
   }

   for (updatedBucket in bucketsToUpdate.take(bucketsToSendInFirstPacket)) {
      firstPacketBuffer.writeUByte(updatedBucket.id)
      firstPacketBuffer.writeUByte(updatedBucket.data.size.toUByte())
      firstPacketBuffer.write(updatedBucket.data)
   }

   return bucketsToUpdate.drop(bucketsToSendInFirstPacket)
}

private fun List<Bucket>.calculateNumUpdatesThatFitInto(bytes: Int): Int {
   var dataLeft = bytes
   var count = 0
   for (updatedBucket in this) {
      val sizeToSend = 2 + updatedBucket.data.size
      if (sizeToSend <= dataLeft) {
         dataLeft -= sizeToSend
         count++
      } else {
         break
      }
   }

   return count
}

private const val SIZE_OF_STATIC_PART_OF_FOLLOW_UP_SYNC_PACKET =
   1 +
      7 + 1 +
      7 + 1
