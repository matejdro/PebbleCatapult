package com.matejdro.catapult.di

import android.app.Service
import dev.zacsweers.metro.MapKey
import kotlin.reflect.KClass

@MapKey
@Target(AnnotationTarget.CLASS)
annotation class ServiceKey(val value: KClass<out Service>)
