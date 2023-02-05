package ru.yandex.yandexnavi.logger

object Logger {
    var lastLogged: String? = null

    @JvmStatic
    fun log(tag: String, message: String) {
        lastLogged = "$tag: $message"
    }
}
