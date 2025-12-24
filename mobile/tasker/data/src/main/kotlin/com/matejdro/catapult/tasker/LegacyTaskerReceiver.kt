package com.matejdro.catapult.tasker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import logcat.logcat

class LegacyTaskerReceiver : BroadcastReceiver() {
   override fun onReceive(context: Context, intent: Intent) {
      if (isOrderedBroadcast()) {
         setResultCode(TaskerPluginConstants.RESULT_CODE_PENDING)
      }

      val serviceIntent: Intent = Intent(context, TaskerActionService::class.java)
      intent.extras?.let {
         it.putBoolean(TaskerPluginConstants.EXTRA_CAN_BIND_FIRE_SETTING, false)

         serviceIntent.putExtras(it)
      }
      logcat { "Received tasker broadcast, starting service" }
      ContextCompat.startForegroundService(context, serviceIntent)
   }
}
