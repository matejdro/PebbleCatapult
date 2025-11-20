plugins {
   pureKotlinModule
   testHelpers
}

dependencies {
   api(projects.tasklist.api)
   api(libs.kotlin.coroutines)
   implementation(libs.kotlinova.core)
}
