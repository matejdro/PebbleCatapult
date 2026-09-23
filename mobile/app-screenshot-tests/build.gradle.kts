plugins {
   androidLibraryModule
   compose
   alias(libs.plugins.paparazzi)
}

android {
   namespace = "com.matejdro.catapult.screenshottests"

   androidResources.enable = true

   testOptions {
      unitTests.all {
         it.useJUnit()

         val numSplits = 3 // How many TestsX classes are there
         it.maxParallelForks = minOf(Runtime.getRuntime().availableProcessors(), numSplits)
         it.systemProperty("numSplits", numSplits)
      }
   }
}

dependencyAnalysis {
   issues {
      onIncorrectConfiguration {
         // screenshot tests need to include app as implementation, otherwise resources do not work properly
         exclude(":app")
      }

      onModuleStructure {
         // False positive
         severity("ignore")
      }
   }
}

dependencies {
   implementation(projects.app)
   testImplementation(libs.junit4)
   testImplementation(libs.junit4.parameterinjector)
   testImplementation(libs.showkase)
}
