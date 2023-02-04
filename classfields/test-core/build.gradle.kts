plugins {
    `ara-core-convention`
}

autoru {
    checkModuleNaming = false
}

dependencies {
    api(projects.testextension)
    api(libs.robolectric) {
        exclude(group = "com.google.protobuf")
    }
    api(libs.androidx.test.junitExt)
    implementation(libs.kotlin.stdlib)
    implementation(libs.rxAndroid)
    implementation(libs.rxJava)

    api(libs.allureKotlin.android)
    implementation(libs.androidx.appcompat)
}
