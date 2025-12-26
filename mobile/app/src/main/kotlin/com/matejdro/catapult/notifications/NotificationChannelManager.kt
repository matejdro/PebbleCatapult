package com.matejdro.catapult.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService
import com.matejdro.catapult.R
import com.matejdro.catapult.common.NotificationsKeys

class NotificationChannelManager(private val context: Context) {
   fun createChannels() {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
         val notificationManager = context.getSystemService<NotificationManager>()!!

         notificationManager.createNotificationChannel(
            NotificationChannel(
               NotificationsKeys.CHANNEL_ID_TASKER_SERVICE,
               context.getString(R.string.channel_background_work),
               NotificationManager.IMPORTANCE_LOW
            )
         )

         notificationManager.createNotificationChannel(
            NotificationChannel(
               NotificationsKeys.CHANNEL_ID_ERRORS,
               context.getString(R.string.channel_errors),
               NotificationManager.IMPORTANCE_HIGH
            )
         )
      }
   }
}
