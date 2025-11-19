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
   implementation(projects.commonCompose)
   api(libs.kotlinova.navigation)
   implementation(libs.accompanist.permissions)
   implementation(libs.androidx.compose.material3.sizeClasses)
   implementation(libs.kotlinova.core)
}
