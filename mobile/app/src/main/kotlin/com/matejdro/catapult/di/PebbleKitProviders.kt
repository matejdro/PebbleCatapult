package com.matejdro.catapult.di

import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.rebble.pebblekit2.client.DefaultPebbleSender
import io.rebble.pebblekit2.client.PebbleSender

@ContributesTo(AppScope::class)
interface PebbleKitProviders {
   @Provides
   @SingleIn(AppScope::class)
   fun providePebbleSender(context: Context): PebbleSender = DefaultPebbleSender(context)
}
