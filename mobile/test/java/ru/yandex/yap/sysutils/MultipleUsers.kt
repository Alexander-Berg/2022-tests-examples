package ru.yandex.yap.sysutils

object MultipleUsers {

    const val USER_OWNER = 0

    var currentUserId = 1 // not an owner by default

    fun reset() {
        currentUserId = 1
    }

    @JvmStatic
    fun getProcessUserId(): Int = currentUserId
}
