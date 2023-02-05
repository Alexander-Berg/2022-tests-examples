package ru.yandex.disk.utils

import android.content.ContentValues

operator fun ContentValues.set(key: String, value: String?) {
    this.put(key, value)
}

operator fun ContentValues.set(key: String, value: Int) {
    this.put(key, value)
}