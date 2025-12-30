plugins {
   androidLibraryModule
   testHelpers
}

dependencies {
   api(projects.bucketsync.api)
   implementation(projects.bucketsync.data)
   implementation(projects.common.test)
   implementation(libs.androidx.datastore.preferences.core)
   implementation(libs.sqldelight.jvm)
}
