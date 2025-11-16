plugins {
   androidLibraryModule
   compose
   parcelize
   showkase
}

android {
   namespace = "com.matejdro.catapult.ui"

   androidResources.enable = true
}

dependencies {
   implementation(libs.kotlinova.core)
   implementation(libs.kotlinova.compose)
   implementation(libs.coil)
   implementation(libs.coil.okhttp)
}
