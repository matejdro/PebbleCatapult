plugins {
   androidLibraryModule
   compose
   di
   navigation
   showkase
}

android {
   namespace = "com.matejdro.catapult.home.ui"

   buildFeatures {
      androidResources = true
   }
}

dependencies {
   api(libs.kotlinova.navigation)
}
