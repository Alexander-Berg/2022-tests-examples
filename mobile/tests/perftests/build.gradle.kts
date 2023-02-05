plugins {
    androidApplication
    kotlinAndroid
    kotlinAndroidExtensions
    kotlinApt
}

android {
    defaultConfig {
        applicationId = "ru.yandex.market.perftests"
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}
apply(from = "perftest.gradle")
dependencies {
    useModule(ProjectModules.featureConfigsCommon)
    useModule(ProjectModules.utils)
    androidTestImplementation(Dependencies.PerfTests.runner)
    androidTestImplementation(Dependencies.AndroidX.Testing.runner)
    androidTestImplementation(Dependencies.AndroidX.Testing.rules)
    androidTestImplementation(Dependencies.gson)
}