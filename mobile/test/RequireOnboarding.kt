package ru.yandex.autotests.mobile.disk.android.rules.annotations.test

/**
 * Indicates that there will be some onboarding opened at the application launch. Otherwise no startup onboarding will be opened.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class RequireOnboarding
