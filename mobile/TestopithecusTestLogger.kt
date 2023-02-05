package com.yandex.mail.metrica

import com.google.gson.Gson
import com.yandex.mail.BuildConfig
import com.yandex.mail.timings.StartupTimeTracker
import com.yandex.xplat.eventus.common.LoggingEvent

object TestopithecusTestLogger {
    private val gson = Gson()
    private val logs = StringBuilder()

    fun log(event: LoggingEvent) {
        if (!BuildConfig.DEBUG) {
            return
        }
        val attributes = mapOf("name" to event.name, "value" to event.attributes)
        val log = gson.toJson(attributes)
        logs.append(log).append("\n")
    }

    fun getLogs(): String {
        return logs.toString()
    }

    fun clear() {
        this.logs.clear()
        StartupTimeTracker.clear()
    }
}
