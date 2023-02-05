package com.yandex.metrica

import android.content.Context

object YandexMetricaInternal {

    var isInitialized = false
    var identifiersRequested = false

    fun clear() {
        isInitialized = false
        identifiersRequested = false
    }

    @JvmStatic
    fun initialize(context: Context, internalConfig: YandexMetricaInternalConfig) {
        isInitialized = true
    }

    @JvmStatic
    fun requestStartupIdentifiers(context: Context, callback: IIdentifierCallback, vararg identifiers: String) {
        identifiersRequested = true
    }
}
