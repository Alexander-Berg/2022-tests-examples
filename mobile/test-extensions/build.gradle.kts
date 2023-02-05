plugins {
    androidLibrary
    kotlinAndroid
    kotlinAndroidExtensions
}

dependencies {
    useModule(ProjectModules.utils)
    useCommonLibraries()
    api(Dependencies.hamcrest)
    api(Dependencies.junit)
    api(Dependencies.Kotlin.reflection)
    api(Dependencies.mockitoKotlin)
    api(Dependencies.StreamApi.testExtensions)
    api(Dependencies.vkAndroidSdk)
    releaseApi(Dependencies.Yandex.Passport.library)
    debugApi(Dependencies.Yandex.Passport.debugLibrary)
}