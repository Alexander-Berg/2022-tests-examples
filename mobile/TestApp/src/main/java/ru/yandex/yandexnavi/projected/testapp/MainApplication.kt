package ru.yandex.yandexnavi.projected.testapp

import android.app.Application
import android.util.Log
import com.yandex.datasync.DatabaseManagerFactory
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.StyleType
import com.yandex.metrica.IIdentifierCallback
import com.yandex.metrica.YandexMetricaInternal
import com.yandex.metrica.YandexMetricaInternalConfig
import com.yandex.navikit.LocalizedString
import com.yandex.navikit.NaviKitLibrary
import com.yandex.navikit.RouteSuggest
import com.yandex.navikit.guidance.EmptyGuidanceListener
import com.yandex.navikit.guidance.Guidance
import com.yandex.navikit.guidance.automotive.AutomotiveGuidanceConsumer
import com.yandex.navikit.guidance.generic.GenericGuidanceComponent
import com.yandex.navikit.notifications.initializeNotificationChannels
import com.yandex.runtime.Runtime
import ru.yandex.yandexnavi.analytics.Analytics
import ru.yandex.yandexnavi.analytics.full.AnalyticsConfig
import ru.yandex.yandexnavi.analytics.full.applyConfig
import ru.yandex.yandexnavi.common.process.ProcessUtils
import ru.yandex.yandexnavi.projected.platformkit.ProjectedKit
import ru.yandex.yandexnavi.projected.testapp.di.AppComponent
import ru.yandex.yandexnavi.projected.testapp.di.AppModule
import ru.yandex.yandexnavi.projected.testapp.di.DaggerAppComponent
import ru.yandex.yandexnavi.projected.testapp.impl.LifecycleCallbacksImpl
import timber.log.Timber
import timber.log.Timber.DebugTree
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

private const val API_KEY = "36614185-aa6d-4547-8ddf-ef1df302b533"

class MainApplication : Application() {
    companion object {
        lateinit var component: AppComponent
            private set
    }

    @Inject
    lateinit var lifecycleCallbacksImpl: LifecycleCallbacksImpl

    @Inject
    lateinit var guidance: Guidance

    @Inject
    lateinit var consumer: AutomotiveGuidanceConsumer

    @Inject
    lateinit var routeSuggest: RouteSuggest

    private val locationListener = object : EmptyGuidanceListener() {
        override fun onLocationUpdated() {
            routeSuggest.reportLocation(guidance.location)
        }
    }

    private val identifierCallback = object : IIdentifierCallback {
        override fun onReceive(params: Map<String, String>) {
            routeSuggest.setClientIdentifiers(
                params.getValue(IIdentifierCallback.YANDEX_MOBILE_METRICA_UUID),
                params.getValue(IIdentifierCallback.YANDEX_MOBILE_METRICA_DEVICE_ID)
            )
        }

        override fun onRequestError(p0: IIdentifierCallback.Reason?) {
        }
    }

    override fun onCreate() {
        super.onCreate()

        initMetrica()
        if (!ProcessUtils.isMainProcess(this)) {
            return
        }

        Timber.plant(DebugTree())

        MapKitFactory.setApiKey(API_KEY)
        MapKitFactory.initialize(applicationContext)
        MapKitFactory.getInstance().setStyleType(StyleType.V_NAV2)

        DatabaseManagerFactory.setApiKey(API_KEY)
        DatabaseManagerFactory.initialize(this)

        NaviKitLibrary.initRoutePreprocessing(this)
        LocalizedString.init(R.string::class.java)
        initializeNotificationChannels(this)

        Runtime.setFailedAssertionListener { file, line, condition, message, stack ->
            val reportMessage = StringBuilder("Assertion failed in $file:$line ($condition, $message). Backtrace: ")

            for (elem in stack) {
                reportMessage.append("\n\t$elem")
            }

            Log.wtf("ProjectedFailedAssertionListener", reportMessage.toString())
        }

        component = DaggerAppComponent.builder()
            .appModule(AppModule(this))
            .build()
        component.inject(this)
        guidance.addGuidanceListener(locationListener)
        GenericGuidanceComponent.startService(this)
        GenericGuidanceComponent.getGenericGuidance().registerConsumer(consumer)
        // TODO route suggest lifecycle
        routeSuggest.resume()

        YandexMetricaInternal.initialize(
            this,
            YandexMetricaInternalConfig.newInternalConfigBuilder("3032ea8b-2adc-4d21-b9ed-6cf0d07033d2")
                .withAppVersion("1")
                .build()
        )
        YandexMetricaInternal.requestStartupIdentifiers(this, identifierCallback)

        registerActivityLifecycleCallbacks(lifecycleCallbacksImpl)
        ProjectedKit.initialize { component }
        guidance.start(null)
    }

    private fun initMetrica() {
        val version = BuildConfig.VERSION_NAME + "(" + BuildConfig.VERSION_CODE + ")"

        val analyticsConfig = AnalyticsConfig(
            this,
            BuildConfig.API_KEY,
            version,
            isCrashlyticsEnabled = false,
            flavor = null,
            buildTime = SimpleDateFormat("dd.MM.yy", Locale.US)
                .format(Date(BuildConfig.BUILD_TIME))
        )

        Analytics.initializer().applyConfig(analyticsConfig).initialize()
    }
}
