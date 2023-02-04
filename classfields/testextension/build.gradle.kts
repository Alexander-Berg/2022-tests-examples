plugins {
    `ara-core-jvm-convention`
}

autoru {
    checkModuleNaming = false
}

dependencies {
    api(libs.junit.junit4)

    api(libs.assertjCore)
    api(libs.mockito.core)
    api(libs.mockito.kotlin) {
        exclude(group = "org.jetbrains.kotlin")
    }
    api(libs.kotlin.reflect)
    api(libs.kotlinTest.base)
    api(libs.kotlinTest.junit)
    api(libs.kotest.runnerJunit5)
    api(libs.kotest.assertionsCore)
    api(libs.kotest.property)
    api(libs.kotest.extensionsAllure)
    api(libs.rxJava)
    api(libs.junit.engine.vintage) // to run junit 3 and 4
    api(libs.junit.engine.jupiter)

    api(libs.junit.platform.launcher)
    api(libs.junit.platform.runner)
    api(libs.junit.platform.commons)

    api(libs.gson)

    api(libs.allureKotlin.model)
    api(libs.allureKotlin.commons)
    api(libs.allureKotlin.junit4)
    api(libs.fixture)
}
