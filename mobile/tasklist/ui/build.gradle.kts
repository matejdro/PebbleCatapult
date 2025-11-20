plugins {
   androidLibraryModule
   compose
   di
   navigation
   showkase
}

android {
   namespace = "com.matejdro.catapult.tasklist.ui"

   buildFeatures {
      androidResources = true
   }
}

dependencies {
   api(projects.common)
   api(projects.tasklist.api)
   api(libs.kotlinova.core)
   api(libs.kotlinova.navigation)
   api(libs.kotlin.coroutines)

   implementation(projects.commonCompose)

   testImplementation(projects.tasklist.test)
   testImplementation(libs.kotlinova.core.test)
}
