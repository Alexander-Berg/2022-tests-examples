package ru.yandex.autotests.mobile.disk.android.rules.annotations.test

/**
 * Indicates that application will not have WRITE_STORAGE permission
 */
@Target(AnnotationTarget.CLASS)
annotation class DoNotGrantPermissionsAutomatically
