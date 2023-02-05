package com.yandex.xplat.testopithecus.common

import com.yandex.xplat.common.Logger

object JavaLogger : Logger {
    override fun info(message: String) {
        println(message)
    }

    override fun warn(message: String) {
        println(message)
    }

    override fun error(message: String) {
        System.err.println(message)
    }
}
