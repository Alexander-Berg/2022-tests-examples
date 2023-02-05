package com.yandex.xplat.testopithecus.common

object ThreadSleep : SyncSleep {
    override fun sleepMs(milliseconds: Int) {
        Thread.sleep(milliseconds.toLong())
    }
}
