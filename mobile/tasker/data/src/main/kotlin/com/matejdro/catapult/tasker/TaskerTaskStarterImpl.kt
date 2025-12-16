package com.matejdro.catapult.tasker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import net.dinglisch.android.tasker.TaskerIntent
import si.inova.kotlinova.core.logging.logcat

@Inject
@ContributesBinding(AppScope::class)
class TaskerTaskStarterImpl(private val context: Context) : TaskerTaskStarter {
   override fun startTask(task: String): Boolean {
      val status = TaskerIntent.testStatus(context)
      logcat { "Tasker status $status" }

      val success = when (status) {
         TaskerIntent.Status.NotInstalled,
         TaskerIntent.Status.NoReceiver,
         -> {
            showErrorNotification(context.getString(R.string.tasker_not_installed))
            false
         }

         TaskerIntent.Status.NotEnabled -> {
            showErrorNotification(context.getString(R.string.tasker_is_disabled))
            false
         }

         TaskerIntent.Status.AccessBlocked -> {
            showErrorNotification(context.getString(R.string.tasker_access_blocked))
            false
         }

         TaskerIntent.Status.OK, null -> true
      }

      if (!success) {
         return false
      }

      val intent = TaskerIntent(task)

      // Regular broadcasts are sometimes delayed on Android 14+. Use ordered ones instead.
      // https://stackoverflow.com/questions/77842817/slow-intent-broadcast-delivery-on-android-14
      context.sendOrderedBroadcast(intent, null)

      return true
   }

   private fun showErrorNotification(message: String) {
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
         .setContentText(message)
         .setSmallIcon(com.matejdro.catapult.sharedresources.R.drawable.ic_launcher)
         .build()

      notificationManager.notify(1, notification)
   }
}

private const val CHANNEL_ID_ERRORS = "ERRORS"
