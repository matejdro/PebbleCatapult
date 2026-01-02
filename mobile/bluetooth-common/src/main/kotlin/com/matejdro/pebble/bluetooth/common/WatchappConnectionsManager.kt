package com.matejdro.pebble.bluetooth.common

import io.rebble.pebblekit2.common.model.PebbleDictionary
import io.rebble.pebblekit2.common.model.ReceiveResult
import io.rebble.pebblekit2.common.model.WatchIdentifier
import java.util.UUID

interface WatchappConnectionsManager {
   public suspend fun onMessageReceived(
      watchappUUID: UUID,
      data: PebbleDictionary,
      watch: WatchIdentifier,
   ): ReceiveResult

   public fun onAppOpened(
      watchappUUID: UUID,
      watch: WatchIdentifier,
   )

   public fun onAppClosed(
      watchappUUID: UUID,
      watch: WatchIdentifier,
   )
}
