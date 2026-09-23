import jacoco.setupJacocoMergingRoot

// Please do not add any subprojects {} / allprojects {} blocks or anything else that affects suborpojects to allow for
// project isolation when it comes out (https://gradle.github.io/configuration-cache/#project_isolation)

plugins {
   id("com.autonomousapps.dependency-analysis")
   id("kotlinova")
   jacoco
}

setupJacocoMergingRoot()

jacoco {
   toolVersion = libs.versions.jacoco.get()
}

dependencyAnalysis {
   structure {
      ignoreKtx(true)

      bundle("coil") {
         // We only ever want coil-compose, so coil is considered as a group
         includeGroup("io.coil-kt.coil3")
      }

      bundle("compose") {
         // Compose libraries are blanket-included to for convenience. It shouldn't cause a big issue
         includeGroup("androidx.compose.animation")
         includeGroup("androidx.compose.foundation")
         includeGroup("androidx.compose.material")
         includeGroup("androidx.compose.material3")
         includeGroup("androidx.compose.runtime")
         includeGroup("androidx.compose.ui")
      }

      // Library Groups:

      bundle("androidxActivity") {
         includeGroup("androidx.activity")
      }

      bundle("androidxBenchmark") {
         includeGroup("androidx.benchmark")
      }

      bundle("androidxCore") {
         includeGroup("androidx.core")
      }

      bundle("androidxLifecycle") {
         includeGroup("androidx.lifecycle")
      }

      bundle("androidxTest") {
         includeGroup("androidx.test")
      }

      bundle("inject") {
         includeGroup("me.tatarka.inject")
         includeGroup("software.amazon.lastmile.kotlin.inject.anvil")
      }

      bundle("datastore") {
         includeGroup("androidx.datastore")
      }

      bundle("kotest") {
         includeGroup("io.kotest")
      }

      bundle("showkase") {
         includeGroup("com.airbnb.android")
      }

      bundle("sqlDelight") {
         includeGroup("app.cash.sqldelight")
      }

      bundle("dagger") {
         includeGroup("com.google.dagger")
         includeDependency("javax.inject:javax.inject")
      }

      bundle("kermit") {
         includeGroup("co.touchlab")
      }

      bundle("tinylog") {
         includeGroup("org.tinylog")
      }

      bundle("windowManagement") {
         includeGroup("androidx.window")
         includeDependency("com.google.accompanist:accompanist-adaptive")
      }

      bundle("PebbleKit2") {
         includeGroup("io.rebble.pebblekit2")
      }

      bundle("Navigation3") {
         includeGroup("androidx.navigation3")
      }

      bundle("WorkManager") {
         includeGroup("androidx.work")
         includeDependency("com.google.guava:listenablefuture")
      }
   }
}

// Always update to the ALL distribution when updating Gradle
tasks.wrapper {
   distributionType = Wrapper.DistributionType.ALL
}
