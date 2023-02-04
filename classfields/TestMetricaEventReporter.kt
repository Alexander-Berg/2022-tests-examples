package com.yandex.mobile.realty.core.metrica

import com.google.gson.Gson
import com.yandex.mobile.realty.analytics.MetricaEventReporter
import com.yandex.mobile.realty.analytics.UserAnalyticsSession

/**
 * @author misha-kozlov on 21.06.2022
 */
class TestMetricaEventReporter(
    gson: Gson,
    userSession: UserAnalyticsSession
) : MetricaEventReporter(gson, userSession) {

    var dispatcher: MetricaDispatcherRegistry? = null

    override fun reportEvent(eventName: String) {
        dispatcher?.dispatch(RecordedEvent(eventName))
    }

    override fun reportEvent(eventName: String, jsonValue: String) {
        dispatcher?.dispatch(RecordedEvent(eventName, jsonValue))
    }
}
