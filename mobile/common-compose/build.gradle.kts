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
   implementation(libs.androidx.core)
   implementation(libs.kotlinova.core)
   implementation(libs.kotlinova.compose)
   implementation(libs.kotlin.coroutines)
   implementation(libs.coil)
   implementation(libs.composeDnd)
   implementation(libs.coil.okhttp)
}
