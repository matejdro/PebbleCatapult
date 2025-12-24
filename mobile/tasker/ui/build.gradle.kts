plugins {
   androidLibraryModule
   compose
   di
   navigation
   parcelize
   showkase
}

android {
   namespace = "com.matejdro.catapult.tasker.ui"

   buildFeatures {
      androidResources = true
   }
}

dependencies {
   api(projects.actionlist.api)
   api(projects.common)
   api(libs.kotlinova.navigation)

   implementation(projects.commonAndroid)
   implementation(projects.commonCompose)
   implementation(projects.commonNavigation)
   implementation(projects.sharedResources)
   implementation(projects.tasker.api)
   implementation(libs.androidx.activity.compose)
   implementation(libs.androidx.core)
   implementation(libs.kotlin.coroutines)
   implementation(libs.kotlinova.core)
   implementation(libs.kotlinova.navigation.navigation3)
   implementation(libs.androidx.navigation3)

   testImplementation(projects.actionlist.test)
   testImplementation(libs.kotlinova.core.test)
   testImplementation(libs.turbine)
}
