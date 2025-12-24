package com.matejdro.catapult.tasker

import android.app.ForegroundServiceStartNotAllowedException
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
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
      try {
         ContextCompat.startForegroundService(context, serviceIntent)
      } catch (e: Exception) {
         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && e is ForegroundServiceStartNotAllowedException) {
            // There is no easy way to solve this, so we just work around by telling user to not use zero-timeout
            // actions, which trigger broadcast receivers instead of services
            // See https://github.com/joaomgcd/TaskerPluginSample/issues/20
            logcat { "Foreground start failed, showing error notification" }
            showErrorNotification(context)
         } else {
            throw e
         }
      }
   }

   private fun showErrorNotification(context: Context) {
      val notificationManager = context.getSystemService<NotificationManager>()!!

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
         notificationManager.createNotificationChannel(
            NotificationChannel(
               CHANNEL_ID_ERRORS,
               context.getString(R.string.channel_errors),
               NotificationManager.IMPORTANCE_HIGH
            )
         )
      }

      val notification = NotificationCompat.Builder(context, CHANNEL_ID_ERRORS)
         .setContentTitle(
            context.getString(
               R.string.notification_title_error,
            )
         )
         .setContentText(context.getString(R.string.error_tasker_zero_timeout))
         .setSmallIcon(com.matejdro.catapult.sharedresources.R.drawable.ic_launcher)
         .build()

      notificationManager.notify(1, notification)
   }
}
