import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlinJvm
    kotlinApt
}

tasks.withType<KotlinCompile>().all {
    kotlinOptions.freeCompilerArgs += "-Xopt-in=com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview"
}

dependencies {
    useModule(ProjectModules.testInstancesProcessorAnnotations)
    useModule(ProjectModules.annotationProcessorCommons)
    useDagger()
    implementation(Dependencies.Kotlin.reflection)
    implementation(Dependencies.KotlinPoet.core)
    implementation(Dependencies.KotlinPoet.metadata)
    implementation(Dependencies.AndroidX.annotations)
    implementation(Dependencies.AutoService.annotations)
    kapt(Dependencies.AutoService.processor)
    implementation(Dependencies.GradleIncapHelper.library)
    kapt(Dependencies.GradleIncapHelper.compiler)
}