package com.matejdro.bucketsync.api

data class BucketUpdate(
   val toVersion: UShort,
   val activeBuckets: List<UShort>,
   val bucketsToUpdate: List<Bucket>,
)

data class Bucket(
   val id: UByte,
   val data: ByteArray,
) {
   override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other !is Bucket) return false

      if (id != other.id) return false
      if (!data.contentEquals(other.data)) return false

      return true
   }

   override fun hashCode(): Int {
      var result = id.hashCode()
      result = 31 * result + data.contentHashCode()
      return result
   }
}
