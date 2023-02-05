package ru.yandex.market.di

import android.app.Activity
import android.content.Context
import com.google.gson.JsonObject
import com.yandex.metrica.RtmConfig
import ru.yandex.market.analytics.appmetrica.AppMetricaTransport
import ru.yandex.market.analytics.appmetrica.AppMetricsAccountType

class TestAppMetricaTransport : AppMetricaTransport {

    override fun reportAppOpen(deeplink: String) {
        // no-op
    }

    override fun sendEvent(eventName: String, jsonEventData: JsonObject) {
        // no-op
    }

    override fun sendEvent(eventName: String) {
        // no-op
    }

    override fun loadClids(context: Context) {
        // no-op
    }

    override fun reportAppOpen(activity: Activity) {
        // no-op
    }

    override fun setPassportUidAndType(uid: String?, type: AppMetricsAccountType) {
        // no-ap
    }

    override fun setPassportUid(uid: String) {
        // no-op
    }

    override fun removePassportUid() {
        // no-op
    }

    override fun updateRtmConfig(rtmConfig: RtmConfig) {
        // no-op
    }
}
