package ru.yandex.yandexnavi.analytics.full

import android.app.ActivityManager

internal object ActivityManagerMock {
    internal var processName = "yandex.auto"

    fun reset() {
        processName = "yandex.auto"
    }
}

internal fun ActivityManager.getCurrentProcessName(): String? = ActivityManagerMock.processName
