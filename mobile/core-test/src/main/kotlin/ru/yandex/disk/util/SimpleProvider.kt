package ru.yandex.disk.util

import javax.inject.Provider

fun <T> T.asProvider() = SimpleProvider(this)

class SimpleProvider<T>(private val obj : T) : Provider<T> {

    override fun get(): T {
        return obj
    }
}
