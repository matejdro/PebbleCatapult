package com.matejdro.bucketsync.di

import androidx.work.ListenableWorker
import dev.zacsweers.metro.MapKey
import kotlin.reflect.KClass

@MapKey
annotation class BucketSyncWorkerKey(val value: KClass<out ListenableWorker>)
