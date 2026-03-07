plugins {
   pureKotlinModule
   testFixtures
}

dependencies {
   api(libs.kotlinova.core)
   api(libs.kotlin.coroutines)
   compileOnly(libs.androidx.compose.runtime.annotation)

   testFixturesApi(projects.actionlist.api)
   testFixturesApi(libs.kotlin.coroutines)
   testFixturesImplementation(libs.kotlinova.core)
}
