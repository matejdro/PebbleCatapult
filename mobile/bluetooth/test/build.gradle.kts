plugins {
   pureKotlinModule
   testHelpers
}

dependencies {
   api(projects.bluetooth.api)
   api(libs.kotlinova.core)
   api(libs.pebblekit.api)
   implementation(libs.kotlin.coroutines)
}
