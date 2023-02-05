package ru.yandex.autotests.mobile.disk.android.rules.annotations.test

/**
 * Used for viewer trailer tests. For these tests viewer trailer would be opening easier on emulator because it has motion event troubles
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class EasyViewerTrailer
