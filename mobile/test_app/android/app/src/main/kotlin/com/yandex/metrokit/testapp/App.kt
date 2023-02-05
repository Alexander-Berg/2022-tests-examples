package com.yandex.metrokit.testapp

import android.app.Application
import android.util.Log
import com.yandex.metrokit.CountryCode
import com.yandex.metrokit.DebugOptions
import com.yandex.metrokit.DebugOptionsFactory
import com.yandex.metrokit.Language
import com.yandex.metrokit.LogLevel
import com.yandex.metrokit.LogWriter
import com.yandex.metrokit.LoggerFactory
import com.yandex.metrokit.metrokit.MetroKit
import com.yandex.metrokit.metrokit.MetroKitFactory
import com.yandex.runtime.Runtime
import java.util.Locale

class App : Application() {

    private val metroKitLogger = LogWriter { nativeLogLevel, source, message ->
        val platformLogLevel = when (nativeLogLevel) {
            LogLevel.INFO -> Log.INFO
            LogLevel.WARNING -> Log.WARN
            LogLevel.DEBUG -> Log.DEBUG
            LogLevel.ERROR -> Log.ERROR
            else -> Log.DEBUG
        }
        Log.println(platformLogLevel, "MetroKit [$source]", message)
    }

    private val debugListener = object : DebugOptions.Listener {
        override fun onApiAssertionFailed() {
            throw RuntimeException("Incorrect API usage detected")
        }
    }

    companion object {
        lateinit var metroKit: MetroKit
            private set
    }

    override fun onCreate() {
        super.onCreate()

        if (Runtime.isMainProcess(this)) {
            Runtime.init(this)
            Runtime.loadLibrary(this, "com.yandex.metrokit")

            DebugOptionsFactory.getDebugOptionsInstance()?.addListener(debugListener)

            val metroKitBuilder = MetroKitFactory.builder()
            val defaultLocale = Locale.getDefault()
            val defaultLanguage = defaultLocale.language
            val defaultCountry = defaultLocale.country
            metroKit = metroKitBuilder.build(Language(defaultLanguage), CountryCode(defaultCountry))

            if (BuildConfig.DEBUG) {
                LoggerFactory.getLoggerInstance().addWriter(metroKitLogger)
            }
        }
    }
}
