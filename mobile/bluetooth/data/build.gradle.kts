plugins {
   pureKotlinModule
   di
}

dependencies {
   api(projects.bluetooth.api)
   api(libs.pebblekit.api)

   implementation(libs.kotlinova.core)
   implementation(libs.kotlin.coroutines)

   testImplementation(projects.bluetooth.test)
   testImplementation(libs.kotlinova.core.test)
}
