plugins {
   pureKotlinModule
   testFixtures
}

dependencies {
   testFixturesApi(projects.bluetooth.api)
   testFixturesApi(projects.bucketsync.api)
   testFixturesApi(libs.pebblekit.api)
   testFixturesImplementation(libs.kotlin.coroutines)
}
