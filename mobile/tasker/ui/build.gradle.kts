plugins {
   androidLibraryModule
   compose
   di
}

android {
   namespace = "com.matejdro.catapult.tasker.ui"

   buildFeatures {
      androidResources = true
   }
}

dependencies {
   api(projects.tasker.api)

   testImplementation(projects.common.test)
}
