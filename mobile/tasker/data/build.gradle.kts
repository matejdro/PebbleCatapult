plugins {
   androidLibraryModule
   di
}

dependencies {
   api(projects.tasker.api)

   implementation(libs.androidx.core)
}
