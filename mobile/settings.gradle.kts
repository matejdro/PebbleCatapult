pluginManagement {
   repositories {
      google()
      mavenCentral()
      gradlePluginPortal()
   }
}

dependencyResolutionManagement {
   repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

   repositories {
      mavenLocal()
      google()
      mavenCentral()
      maven("https://jitpack.io")
   }

   versionCatalogs {
      create("libs") {
         from(files("config/libs.toml"))
      }
   }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "PebbleCatapult"

include(":app")
include(":app-screenshot-tests")
include(":common")
include(":common:test")
include(":common-android")
include(":common-android:test")
include(":common-compose")
include(":common-navigation")
include(":detekt")
include(":logging:api")
include(":logging:data")
include(":logging:crashreport")
include(":home:ui")
include(":shared-resources")
include(":actionlist:api")
include(":actionlist:data")
include(":actionlist:ui")
include(":actionlist:test")
include(":tools:ui")
