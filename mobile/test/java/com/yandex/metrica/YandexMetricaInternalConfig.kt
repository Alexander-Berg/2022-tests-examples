package com.yandex.metrica

class YandexMetricaInternalConfig {
    companion object {

        var version: String? = null
        var crashReportEnabled = false
        var logsEnabled = false

        val params = mutableMapOf<String, String?>()

        fun clear() {
            version = null
            crashReportEnabled = false
            logsEnabled = false
            params.clear()
        }

        @JvmStatic
        fun newBuilder(apiKey: String): Builder {
            return Builder(apiKey)
        }
    }

    class Builder internal constructor(private val apiKey: String) {

        fun withAppVersion(appVersion: String): Builder {
            version = appVersion
            return this
        }

        fun withCrashReporting(enabled: Boolean): Builder {
            crashReportEnabled = true
            return this
        }

        fun withLogs(): Builder {
            logsEnabled = true
            return this
        }

        fun withAppEnvironmentValue(key: String, value: String?): Builder {
            params[key] = value
            return this
        }

        fun build(): YandexMetricaInternalConfig {
            return YandexMetricaInternalConfig()
        }
    }
}
