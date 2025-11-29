plugins {
   androidLibraryModule
   compose
   di
   navigation
   parcelize
   showkase
}

android {
   namespace = "com.matejdro.catapult.actionlist.ui"

   buildFeatures {
      androidResources = true
   }
}

dependencies {
   api(projects.common)
   api(projects.actionlist.api)
   api(libs.kotlinova.core)
   api(libs.kotlinova.navigation)
   api(libs.kotlin.coroutines)

   implementation(projects.commonCompose)
   implementation(projects.sharedResources)
   implementation(libs.androidx.activity.compose)

   testImplementation(projects.actionlist.test)
   testImplementation(libs.kotlinova.core.test)
}
