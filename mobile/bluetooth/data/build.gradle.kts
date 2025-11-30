plugins {
   pureKotlinModule
   di
}

dependencies {
   api(projects.bluetooth.api)
   api(projects.bucketsync.api)
   api(projects.actionlist.api)
   api(libs.pebblekit.api)

   implementation(libs.kotlinova.core)
   implementation(libs.kotlin.coroutines)
   implementation(libs.dispatch)
   implementation(libs.okio)

   testImplementation(projects.bluetooth.test)
   testImplementation(projects.bucketsync.test)
   testImplementation(projects.actionlist.test)
   testImplementation(libs.kotlinova.core.test)
}
