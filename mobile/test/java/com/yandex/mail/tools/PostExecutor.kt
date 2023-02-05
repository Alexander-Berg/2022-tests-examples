package com.yandex.mail.tools

import android.os.Handler
import java.util.concurrent.Executor

class PostExecutor(private val handler: Handler) : Executor {
    override fun execute(command: Runnable) {
        handler.post(command)
    }
}
