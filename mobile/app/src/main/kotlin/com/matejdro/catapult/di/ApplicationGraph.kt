package com.matejdro.catapult.di

import android.app.Application
import com.matejdro.catapult.MainViewModel
import com.matejdro.catapult.bluetooth.WatchSyncer
import com.matejdro.catapult.logging.FileLoggingController
import com.matejdro.catapult.logging.TinyLogLoggingThread
import com.matejdro.catapult.navigation.scenes.TabListDetailScene
import com.matejdro.catapult.receiving.PebbleListenerService
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Multibinds
import dev.zacsweers.metro.Provides
import dispatch.core.DefaultCoroutineScope
import si.inova.kotlinova.core.reporting.ErrorReporter
import si.inova.kotlinova.core.time.AndroidDateTimeFormatter
import si.inova.kotlinova.navigation.conditions.ConditionalNavigationHandler
import si.inova.kotlinova.navigation.deeplink.MainDeepLinkHandler
import si.inova.kotlinova.navigation.di.NavigationContext
import si.inova.kotlinova.navigation.di.NavigationInjection
import si.inova.kotlinova.navigation.di.OuterNavigationScope
import kotlin.reflect.KClass

@DependencyGraph(AppScope::class, additionalScopes = [OuterNavigationScope::class])
interface MainApplicationGraph : ApplicationGraph {
   @DependencyGraph.Factory
   interface Factory {
      fun create(
         @Provides
         application: Application,
      ): MainApplicationGraph
   }

   @Multibinds(allowEmpty = true)
   fun provideEmptyConditionalMultibinds(): Map<KClass<*>, ConditionalNavigationHandler>
}

@Suppress("ComplexInterface") // DI
interface ApplicationGraph {
   fun getErrorReporter(): ErrorReporter
   fun getDefaultCoroutineScope(): DefaultCoroutineScope
   fun getNavigationInjectionFactory(): NavigationInjection.Factory
   fun getMainDeepLinkHandler(): MainDeepLinkHandler
   fun getNavigationContext(): NavigationContext
   fun getDateFormatter(): AndroidDateTimeFormatter
   fun getMainViewModelFactory(): MainViewModel.Factory
   fun getFileLoggingController(): FileLoggingController
   fun getTinyLogLoggingThread(): TinyLogLoggingThread
   fun getWatchSyncer(): WatchSyncer
   fun getTabListDetailSceneFactory(): TabListDetailScene.Factory

   fun inject(target: PebbleListenerService)
}
