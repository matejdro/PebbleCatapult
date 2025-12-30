plugins {
   androidLibraryModule
   di
   sqldelight
}

sqldelight {
   databases {
      create("Database") {
         packageName.set("com.matejdro.bucketsync.sqldelight.generated")
         schemaOutputDirectory.set(file("src/main/sqldelight/databases"))
      }
   }
}
dependencies {
   api(projects.bucketsync.api)
   api(libs.androidx.datastore.preferences.core)

   implementation(libs.dispatch)
   implementation(libs.kotlinova.core)
   implementation(libs.kotlin.coroutines)
   implementation(libs.logcat)
   implementation(libs.androidx.workManager)
   implementation(libs.pebblekit)

   testImplementation(projects.bucketsync.test)
   testImplementation(projects.common.test)
   testImplementation(libs.kotlinova.core.test)
   testImplementation(libs.turbine)
}
