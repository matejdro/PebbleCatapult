plugins {
   pureKotlinModule
}

dependencies {
   api(libs.kotlinova.core)
   api(libs.kotlin.coroutines)
   compileOnly(libs.androidx.compose.runtime.annotation)
}
