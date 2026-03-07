plugins {
   androidLibraryModule
   compose
   di
   navigation
   parcelize
   serialization
   showkase
}

android {
   namespace = "com.matejdro.catapult.actionlist.ui"

   androidResources.enable = true
}

dependencies {
   api(projects.common)
   api(projects.actionlist.api)
   api(libs.kotlinova.core)
   api(libs.kotlinova.navigation)
   api(libs.kotlin.coroutines)

   implementation(projects.commonCompose)
   implementation(projects.sharedResources)
   implementation(libs.androidx.activity.compose)

   testImplementation(testFixtures(projects.actionlist.api))
   testImplementation(libs.kotlinova.core.test)
}
