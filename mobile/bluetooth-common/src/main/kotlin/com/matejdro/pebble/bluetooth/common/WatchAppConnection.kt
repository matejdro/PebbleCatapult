package com.matejdro.pebble.bluetooth.common

import io.rebble.pebblekit2.common.model.PebbleDictionary
import io.rebble.pebblekit2.common.model.ReceiveResult
import io.rebble.pebblekit2.common.model.WatchIdentifier
import kotlinx.coroutines.CoroutineScope

interface WatchAppConnection {
   suspend fun onPacketReceived(data: PebbleDictionary): ReceiveResult

   fun interface Factory {
      fun create(
         watch: WatchIdentifier,
         scope: CoroutineScope,
      ): WatchAppConnection
   }
}
