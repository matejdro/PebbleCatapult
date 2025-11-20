plugins {
   pureKotlinModule
   di
   sqldelight
}

sqldelight {
   databases {
      create("Database") {
         packageName.set("com.matejdro.catapult.tasklist.sqldelight.generated")
         schemaOutputDirectory.set(file("src/main/sqldelight/databases"))
      }
   }
}

dependencies {
   api(projects.tasklist.api)
   api(libs.kotlin.coroutines)
   implementation(libs.kotlinova.core)
   implementation(libs.dispatch)

   testImplementation(libs.kotlinova.core.test)
   testImplementation(libs.turbine)
}
