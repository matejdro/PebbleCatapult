plugins {
   androidLibraryModule
   di
   unmock
}

android {
   namespace = "com.matejdro.catapult.tasker"

   androidResources.enable = true
}

dependencies {
   api(projects.actionlist.api)
   api(projects.tasker.api)
   api(libs.dispatch)

   implementation(projects.commonAndroid)
   implementation(projects.sharedResources)
   implementation(libs.androidx.core)
   implementation(libs.kotlinova.core)
   implementation(libs.kotlin.coroutines)
   implementation(libs.logcat)

   testImplementation(projects.actionlist.test)
   testImplementation(libs.kotlinova.core.test)
}
