plugins {
   pureKotlinModule
   testHelpers
}

dependencies {
   api(projects.actionlist.api)
   api(libs.kotlin.coroutines)
   implementation(libs.kotlinova.core)
}
