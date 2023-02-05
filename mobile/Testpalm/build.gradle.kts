@Suppress("DSL_SCOPE_VIOLATION", "UnstableApiUsage")
plugins {
    `tool-conventions`
    alias(libs.plugins.kotlinxSerialization)
}

application {
    mainClass.set("ru.yandex.yandexmaps.tools.testpalm.AppKt")
}

dependencies {
    implementation(libs.bundles.ktor)
    implementation(libs.kotlinxCli)
}
