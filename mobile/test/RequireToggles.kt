package ru.yandex.autotests.mobile.disk.android.rules.annotations.test

/**
 * Enables feature toggles
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class RequireToggles(vararg val value: String)
