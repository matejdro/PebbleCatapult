package com.matejdro.catapult.tasker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.getSystemService
import androidx.core.os.bundleOf
import com.matejdro.catapult.common.di.NavigationInjectingApplication
import dev.zacsweers.metro.Inject
import dispatch.core.MainImmediateCoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import logcat.logcat
import net.dinglisch.android.tasker.TaskerPlugin
import si.inova.kotlinova.core.reporting.ErrorReporter
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.cancellation.CancellationException

class TaskerActionService : Service() {
   @Inject
   lateinit var taskerRunner: TaskerActionRunner

   @Inject
   lateinit var errorReporter: ErrorReporter

   @Inject
   lateinit var coroutineScope: MainImmediateCoroutineScope

   private val runningTasks = AtomicInteger(0)

   private val binder by lazy { Binder() }

   override fun onCreate() {
      applicationContext!!
         .let { it as NavigationInjectingApplication }
         .applicationGraph
         .let { it as TaskerServiceInjector }
         .inject(this)

      super.onCreate()
   }

   override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
      val canBind = intent.getBooleanExtra(TaskerPluginConstants.EXTRA_CAN_BIND_FIRE_SETTING, false)
      logcat { "Starting TaskerActionService. Bound: $canBind" }
      if (!canBind) {
         startForeground()
      }

      runningTasks.incrementAndGet()

      coroutineScope.launch {
         try {
            taskerRunner.run(intent.extras ?: Bundle())
            logcat { "Run finished" }

            TaskerPlugin.Setting.signalFinish(
               this@TaskerActionService,
               intent,
               TaskerPluginConstants.RESULT_CODE_OK,
               Bundle()
            )
         } catch (e: CancellationException) {
            TaskerPlugin.Setting.signalFinish(
               this@TaskerActionService,
               intent,
               TaskerPluginConstants.RESULT_CODE_FAILED,
               bundleOf("%err" to "1", "%errmsg" to "Cancelled")
            )
            throw e
         } catch (e: Exception) {
            errorReporter.report(e)
            TaskerPlugin.Setting.signalFinish(
               this@TaskerActionService,
               intent,
               TaskerPluginConstants.RESULT_CODE_FAILED,
               bundleOf("%err" to "1", "%errmsg" to e.message)
            )
         } finally {
            val leftTasks = runningTasks.decrementAndGet()
            if (leftTasks == 0) {
               logcat { "Stopping service" }
               stopSelf()
            }
         }
      }

      return super.onStartCommand(intent, flags, startId)
   }

   override fun onDestroy() {
      coroutineScope.cancel()
   }

   override fun onBind(intent: Intent?): IBinder? {
      return binder
   }

   private fun startForeground() {
      val notificationManager = getSystemService<NotificationManager>()!!

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
         notificationManager.createNotificationChannel(
            NotificationChannel(
               CHANNEL_ID_TASKER_SERVICE,
               getString(R.string.channel_background_work),
               NotificationManager.IMPORTANCE_LOW
            )
         )
      }

      val notification = NotificationCompat.Builder(this, CHANNEL_ID_TASKER_SERVICE)
         .setContentTitle(
            getString(com.matejdro.catapult.sharedresources.R.string.app_name)
         )
         .setContentText(getString(R.string.running_tasker_action))
         .setSmallIcon(com.matejdro.catapult.sharedresources.R.drawable.ic_launcher)
         .build()

      ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
   }
}

private const val CHANNEL_ID_TASKER_SERVICE = "TASKER_SERVICE"
private const val NOTIFICATION_ID = 15476

/**
 * copy of the [ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE] for compat reasons
 */
private const val FOREGROUND_SERVICE_TYPE_SPECIAL_USE = 1 shl 30
