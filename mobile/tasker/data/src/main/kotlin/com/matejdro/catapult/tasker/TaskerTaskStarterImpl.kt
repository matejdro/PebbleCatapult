package com.matejdro.catapult.tasker

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlin.random.Random

@Inject
@ContributesBinding(AppScope::class)
class TaskerTaskStarterImpl(private val context: Context) : TaskerTaskStarter {
   override fun startTask(task: String): Boolean {
      // TODO validate tasker install

      val intent = Intent("net.dinglisch.android.tasker.ACTION_TASK", "id:${Random.nextLong()}".toUri()).apply {
         // `package` = "net.dinglisch.android.tasker"

         putExtra("version_number", "1.1")
         putExtra("task_name", task)
      }

      context.sendBroadcast(intent)

      return true
   }
}
