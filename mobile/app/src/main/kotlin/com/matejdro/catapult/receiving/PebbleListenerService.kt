package com.matejdro.catapult.receiving

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.matejdro.catapult.CatapultApplication
import com.matejdro.catapult.bluetooth.WatchappConnectionsManager
import com.matejdro.catapult.di.ServiceKey
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dispatch.core.DefaultCoroutineScope
import io.rebble.pebblekit2.client.BasePebbleListenerService
import io.rebble.pebblekit2.common.model.PebbleDictionary
import io.rebble.pebblekit2.common.model.ReceiveResult
import io.rebble.pebblekit2.common.model.WatchIdentifier
import java.util.UUID

@ContributesIntoMap(AppScope::class, binding = binding<Service>())
@ServiceKey(PebbleListenerService::class)
@Inject
class PebbleListenerService : BasePebbleListenerService() {
   @Inject
   @Volatile
   override lateinit var coroutineScope: DefaultCoroutineScope

   @Inject
   @Volatile
   @Suppress("VarCouldBeVal") // False positive
   private lateinit var watchappConnectionsManager: WatchappConnectionsManager

   @Volatile
   private var initialized = false

   override fun onBind(intent: Intent?): IBinder? {
      if (!initialized) {
         synchronized(this) {
            if (!initialized) {
               (application!! as CatapultApplication).applicationGraph.inject(this)
               initialized = true
            }
         }
      }

      return super.onBind(intent)
   }

   override suspend fun onMessageReceived(
      watchappUUID: UUID,
      data: PebbleDictionary,
      watch: WatchIdentifier,
   ): ReceiveResult {
      return watchappConnectionsManager.onMessageReceived(watchappUUID, data, watch)
   }

   override fun onAppOpened(watchappUUID: UUID, watch: WatchIdentifier) {
      watchappConnectionsManager.onAppOpened(watchappUUID, watch)
   }

   override fun onAppClosed(watchappUUID: UUID, watch: WatchIdentifier) {
      watchappConnectionsManager.onAppClosed(watchappUUID, watch)
   }
}
