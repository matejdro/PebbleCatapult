plugins {
   androidLibraryModule
   di
}

dependencies {
   api(projects.bluetooth.api)
   api(projects.bluetoothCommon)
   api(projects.bucketsync.api)
   api(projects.actionlist.api)
   api(projects.tasker.api)
   api(libs.pebblekit.api)

   implementation(projects.common)
   implementation(libs.dispatch)
   implementation(libs.kotlinova.core)
   implementation(libs.kotlin.coroutines)
   implementation(libs.logcat)
   implementation(libs.okio)

   testImplementation(projects.bluetooth.test)
   testImplementation(projects.bucketsync.test)
   testImplementation(projects.bucketsync.data)
   testImplementation(projects.actionlist.test)
   testImplementation(projects.tasker.test)
   testImplementation(libs.kotlinova.core.test)
}
