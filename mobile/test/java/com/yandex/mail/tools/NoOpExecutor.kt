package com.yandex.mail.tools

import java.util.concurrent.Executor

class NoOpExecutor : Executor {
    override fun execute(command: Runnable) {
        // no-op
    }
}
