plugins {
   pureKotlinModule
   compose
   serialization
}

dependencies {
   api(libs.kotlinova.navigation)
   implementation(libs.androidx.compose.material3.sizeClasses)
   implementation(libs.kotlinova.compose)
}
