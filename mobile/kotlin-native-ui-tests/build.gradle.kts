
multiplatform {
    kotlin {
        iosFramework("KotlinNativeUITests", listOf(
            ":multiplatform:core",
            ":multiplatform:ui-testing"
        ))
    }
}

