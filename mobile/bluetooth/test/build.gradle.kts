plugins {
   pureKotlinModule
   testHelpers
}

dependencies {
   api(libs.kotlinova.core)
   api(libs.pebblekit.api)
   implementation(libs.kotlin.coroutines)
}
