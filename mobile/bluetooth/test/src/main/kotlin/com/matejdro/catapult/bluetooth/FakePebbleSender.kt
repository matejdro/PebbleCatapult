package com.matejdro.catapult.bluetooth

import io.rebble.pebblekit2.client.PebbleSender
import io.rebble.pebblekit2.common.model.PebbleDictionary
import io.rebble.pebblekit2.common.model.TransmissionResult
import io.rebble.pebblekit2.common.model.WatchIdentifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import si.inova.kotlinova.core.time.TimeProvider
import java.util.UUID

class FakePebbleSender(
   private val timeProvider: TimeProvider,
) : PebbleSender {
   val sentPackets = mutableListOf<SentPacket>()
   private val _pauseSending = MutableStateFlow(false)

   var sendingResult: TransmissionResult = TransmissionResult.Success
   var reportNoPebbleAppInstalled: Boolean = false

   val startedApps = mutableListOf<AppLifecycleEvent>()

   var pauseSending: Boolean
      get() = _pauseSending.value
      set(value) {
         _pauseSending.value = value
      }

   override suspend fun sendDataToPebble(
      watchappUUID: UUID,
      data: PebbleDictionary,
      watches: List<WatchIdentifier>?,
   ): Map<WatchIdentifier, TransmissionResult>? {
      val sentTime = timeProvider.currentTimeMillis()
      _pauseSending.first { !it }

      sentPackets += SentPacket(watchappUUID, data, watches, sentTime)

      if (reportNoPebbleAppInstalled) {
         return null
      }

      return watches?.associateWith { sendingResult }.orEmpty()
   }

   override suspend fun startAppOnTheWatch(
      watchappUUID: UUID,
      watches: List<WatchIdentifier>?,
   ): Map<WatchIdentifier, TransmissionResult>? {
      startedApps += AppLifecycleEvent(watchappUUID, watches)

      return watches?.associateWith { sendingResult }.orEmpty()
   }

   override suspend fun stopAppOnTheWatch(
      watchappUUID: UUID,
      watches: List<WatchIdentifier>?,
   ): Map<WatchIdentifier, TransmissionResult>? {
      throw UnsupportedOperationException("Not supported in tests")
   }

   override fun close() {
      throw UnsupportedOperationException("Not supported in tests")
   }

   data class SentPacket(
      val watchappUUID: UUID,
      val data: PebbleDictionary,
      val watches: List<WatchIdentifier>?,
      val sentTime: Long,
   )

   data class AppLifecycleEvent(
      val watchappUUID: UUID,
      val watches: List<WatchIdentifier>?,
   )
}

val FakePebbleSender.sentData: List<PebbleDictionary>
   get() = sentPackets.map { it.data }
