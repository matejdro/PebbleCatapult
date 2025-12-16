plugins {
   androidLibraryModule
   di
}

android {
   namespace = "com.matejdro.catapult.tasker"

   androidResources.enable = true
}

dependencies {
   api(projects.tasker.api)

   implementation(projects.sharedResources)
   implementation(libs.androidx.core)
   implementation(libs.kotlinova.core)
}
