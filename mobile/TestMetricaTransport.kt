package ru.yandex.market.di

import com.google.gson.Gson
import com.google.gson.JsonObject
import ru.yandex.market.analytics.health.MetricaTransport

class TestMetricaTransport : MetricaTransport {
    val events = mutableSetOf<String>()
    override fun reportEvent(name: String, data: String) {
        val realName = if (name == "HEALTH_EVENT_STATBOX") {
            Gson().fromJson(data, JsonObject::class.java).get("name").asString
        } else {
            name
        }
        events.add(realName)
    }
}