plugins {
    `ara-android-application-base-plugin`
    `ara-compose-mixin`
}

android {
    defaultConfig.applicationId = "ru.auto.test"
}

yandexSigner {
    applicationName = android.defaultConfig.applicationId
}

android {
    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            resValue("string", "app_name", "@string/app_name_debug")
            isDebuggable = false
            manifestPlaceholders["settingsPermissionName"] = "ru.auto.ara.debug.permission.SETTINGS"
        }
        getByName("release") {
            resValue("string", "app_name", "@string/app_name_release")
            isDebuggable = false
            manifestPlaceholders["settingsPermissionName"] = "ru.auto.ara.permission.SETTINGS"
        }
    }
    buildFeatures.resValues = true
}

dependencies {
    implementation(projects.data)
    implementation(projects.dataContract)
    implementation(projects.coreUi)
    implementation(projects.coreUiCompose)
    implementation(projects.settingsProvider)

    implementation(libs.kotlin.stdlib)
    implementation(libs.gson)

    //compose
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material)
    implementation(libs.compose.material3)
    implementation(libs.compose.materialIcons.core)
    implementation(libs.compose.materialIcons.extended)
    implementation(libs.compose.runtime.liveData)
    implementation(libs.compose.runtime.rxJava2)
    implementation(libs.compose.activity)
    implementation(libs.compose.navigation)
    implementation(libs.accompanist.navigation.animation)
}
