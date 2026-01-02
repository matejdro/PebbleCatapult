plugins {
   androidLibraryModule
   di
}

dependencies {
   api(libs.dispatch)
   api(libs.pebblekit.api)

   implementation(projects.common)
   implementation(libs.kotlinova.core)
   implementation(libs.kotlin.coroutines)
   implementation(libs.logcat)
   implementation(libs.okio)

   testImplementation(projects.bluetooth.test)
   testImplementation(projects.bucketsync.test)
   testImplementation(projects.actionlist.test)
   testImplementation(projects.tasker.test)
   testImplementation(libs.kotlinova.core.test)
}
