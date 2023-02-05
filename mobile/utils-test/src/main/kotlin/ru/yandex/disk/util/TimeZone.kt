package ru.yandex.disk.util

import java.util.*

fun withTz(tz: TimeZone, action: () -> Unit) {
    val default = TimeZone.getDefault()
    try {
        TimeZone.setDefault(tz)
        action()

    } finally {
        TimeZone.setDefault(default)
    }
}