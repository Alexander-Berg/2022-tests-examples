package ru.yandex.disk.util

import ru.yandex.util.Path

fun String.asPath() = Path.asPath(this)!!