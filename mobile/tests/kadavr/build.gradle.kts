plugins {
    androidLibrary
    kotlinAndroid
}

android {
    defaultConfig {
        buildConfigField("String", "KADAVR_URL", "\"${rootProject.property("kadavr.url")}\"")
    }
}

dependencies {
    useModule(ProjectModules.utils)
    implementation(Dependencies.Retrofit.library)
    implementation(Dependencies.Retrofit.gsonConverter)
    implementation(Dependencies.Kotlin.StdLib.jdk7)

    testImplementation(Dependencies.Kotlin.reflection)
    testImplementation(Dependencies.mockitoKotlin)

    androidTestImplementation(Dependencies.Kotlin.reflection)
    androidTestImplementation(Dependencies.Kotlin.Tests.core)
    androidTestImplementation(Dependencies.Kotlin.Tests.junit)
}