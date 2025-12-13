import com.slack.keeper.optInToKeeper

plugins {
   androidAppModule
   compose
   navigation
   parcelize
   showkase
   sqldelight
   id("com.slack.keeper")
   id("androidx.baselineprofile")
}

android {
   namespace = "com.matejdro.catapult"

   buildFeatures {
      buildConfig = true
   }

   defaultConfig {
      applicationId = "com.matejdro.catapult"
      targetSdk = 33
      versionCode = 1
      versionName = "1.0.0"

      testInstrumentationRunner = "com.matejdro.catapult.instrumentation.TestRunner"
      testInstrumentationRunnerArguments += "clearPackageData" to "true"
      // Needed to enable test coverage
      testInstrumentationRunnerArguments += "useTestStorageService" to "true"
   }

   testOptions {
      execution = "ANDROIDX_TEST_ORCHESTRATOR"
   }

   if (hasProperty("testAppWithProguard")) {
      testBuildType = "proguardedDebug"
   }

   signingConfigs {
      getByName("debug") {
         // SHA1: 0F:5C:AB:AB:28:7B:F0:22:05:8A:6F:F1:99:2B:95:9D:46:32:02:6D
         // SHA256: 7A:AB:A9:00:84:7D:98:2C:10:1B:2A:03:28:38:1A:D4:62:D5:A3:00:E1:24:25:00:5F:32:0D:E2:C2:5B:D3:B1

         storeFile = File(rootDir, "keys/debug.jks")
         storePassword = "android"
         keyAlias = "androiddebugkey"
         keyPassword = "android"
      }

      create("release") {
         // SHA1: 8F:F7:78:01:4E:36:90:73:6D:7B:EA:13:32:E6:E9:8A:D5:75:0D:E9
         // SHA256: D8:50:87:C7:D0:86:2F:7F:72:2D:10:DB:EB:E4:B7:2E:74:71:CB:00:75:3A:6F:69:36:B8:E9:59:6B:3B:FB:03

         storeFile = File(rootDir, "keys/release.jks")
         storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
         keyAlias = "app"
         keyPassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
      }
   }

   buildTypes {
      getByName("debug") {
         signingConfig = signingConfigs.getByName("debug")
      }

      create("proguardedDebug") {
         isMinifyEnabled = true
         isShrinkResources = true

         proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro",
         )

         testProguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro",
            "proguard-rules-test.pro"
         )

         matchingFallbacks += "debug"

         signingConfig = signingConfigs.getByName("debug")
      }

      create("benchmark") {
         isDebuggable = true
         initWith(buildTypes.getByName("release"))
         signingConfig = signingConfigs.getByName("debug")
         matchingFallbacks += listOf("release")
      }

      getByName("release") {
         isMinifyEnabled = true
         isShrinkResources = true

         proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
         )

         signingConfig = signingConfigs.getByName("release")
      }
   }
}

androidComponents {
   beforeVariants { builder ->
      if (builder.name.contains("proguardedDebug")) {
         builder.optInToKeeper()
      }
   }
}

keeper {
   automaticR8RepoManagement = false
}

custom {
   enableEmulatorTests.set(true)
}

sqldelight {
   databases {
      create("Database") {
         packageName.set("com.matejdro.catapult")
         schemaOutputDirectory.set(file("src/main/sqldelight/databases"))

         // Use project() wrapper as a workaround for the https://github.com/sqldelight/sqldelight/pull/5801
         dependency(project(projects.actionlist.data.path))
         dependency(project(projects.bucketsync.data.path))
      }
   }
}

dependencyAnalysis {
   issues {
      onUnusedDependencies {
         // False positive
         exclude(":common")
      }
   }
}

dependencies {
   implementation(projects.common)
   implementation(projects.commonNavigation)
   implementation(projects.commonCompose)
   implementation(projects.home.ui)
   implementation(projects.logging.api)
   implementation(projects.logging.crashreport)
   implementation(projects.logging.data)
   implementation(projects.actionlist.api)
   implementation(projects.actionlist.data)
   implementation(projects.actionlist.ui)
   implementation(projects.bluetooth.api)
   implementation(projects.bluetooth.data)
   implementation(projects.bucketsync.api)
   implementation(projects.bucketsync.data)
   implementation(projects.tasker.api)
   implementation(projects.tasker.data)
   implementation(projects.tools.ui)

   implementation(libs.androidx.activity.compose)
   implementation(libs.androidx.core)
   implementation(libs.androidx.core.splashscreen)
   implementation(libs.androidx.datastore)
   implementation(libs.androidx.datastore.preferences)
   implementation(libs.androidx.lifecycle.runtime)
   implementation(libs.androidx.lifecycle.viewModel)
   implementation(libs.androidx.lifecycle.viewModel.compose)
   implementation(libs.coil)
   implementation(libs.dispatch)
   implementation(libs.kermit)
   implementation(libs.kotlin.immutableCollections)
   implementation(libs.moshi)
   implementation(libs.kotlin.coroutines)
   implementation(libs.kotlinova.core)
   implementation(libs.kotlinova.navigation)
   implementation(libs.pebblekit)
   implementation(libs.simpleStack)
   implementation(libs.sqldelight.android)
   implementation(libs.tinylog.api)


   keeperR8(libs.androidx.r8)
}
