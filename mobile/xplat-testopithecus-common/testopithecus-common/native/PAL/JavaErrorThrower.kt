package com.yandex.xplat.testopithecus.common

object JavaErrorThrower : ErrorThrower {
    override fun fail(message: String) {
        throw AssertionError(message)
    }
}
