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

   testImplementation(testFixtures(projects.bluetooth.api))
   testImplementation(projects.bucketsync.test)
   testImplementation(projects.bucketsync.data)
   testImplementation(testFixtures(projects.actionlist.api))
   testImplementation(testFixtures(projects.tasker.api))
   testImplementation(libs.kotlinova.core.test)
}
