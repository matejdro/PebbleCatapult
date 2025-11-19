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
   // api(projects.tasklist.api)
}
