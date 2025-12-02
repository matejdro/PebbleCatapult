plugins {
   pureKotlinModule
   di
   sqldelight
}

sqldelight {
   databases {
      create("Database") {
         packageName.set("com.matejdro.catapult.actionlist.sqldelight.generated")
         schemaOutputDirectory.set(file("src/main/sqldelight/databases"))
      }
   }
}

dependencies {
   api(projects.actionlist.api)
   api(projects.bluetooth.api)
   api(libs.kotlin.coroutines)
   implementation(libs.kotlinova.core)
   implementation(libs.dispatch)

   testImplementation(projects.bluetooth.test)
   testImplementation(libs.kotlinova.core.test)
   testImplementation(libs.turbine)
}
