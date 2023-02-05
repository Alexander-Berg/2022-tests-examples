package ru.yandex.yandexnavi.guidance_lib_test_app

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.multidex.MultiDex
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.StyleType
import com.yandex.mapkit.annotations.AnnotationLanguage
import com.yandex.navikit.NaviKitLibrary
import com.yandex.navikit.complex_junctions.ComplexJunctionsConfigData
import com.yandex.navikit.complex_junctions.ComplexJunctionsConfigDataProvider
import com.yandex.navikit.complex_junctions.ComplexJunctionsConfigDataProviderListener
import com.yandex.navikit.guidance.Guidance
import com.yandex.navikit.guidance.SoundMuter
import com.yandex.navikit.guidance.automotive.AutomotiveGuidanceConsumer
import com.yandex.navikit.guidance.automotive.ProjectedStatusChangeListener
import com.yandex.navikit.guidance.automotive.ProjectedStatusProvider
import com.yandex.navikit.guidance.generic.GenericGuidanceComponent
import com.yandex.navikit.guidance.notification.GenericGuidanceNotificationManager
import com.yandex.navikit.night_mode.*
import com.yandex.navikit.notifications.initializeNotificationChannels
import com.yandex.navikit.report.Metrica
import ru.yandex.yandexnavi.ui.guidance.notifications.NotificationPendingIntentProvider
import ru.yandex.yandexnavi.ui.guidance.notifications.createNotificationBuilder
import ru.yandex.yandexnavi.ui.guidance.notifications.createNotificationClickReceiver
import ru.yandex.yandexnavi.ui.guidance.notifications.freedrive.NotificationFreedriveDataProviderImpl

private class PlatformNightModeProviderImpl : PlatformNightModeProvider {
    override fun bindListener(nightModeListener: NativeNightModeListener) = Unit
    override fun isNightMode(): Boolean? = false
}

private class SoundMuterImpl : SoundMuter {
    private var muted = false

    override fun isMuted() = muted

    override fun setMuted(muted: Boolean) {
        this.muted = muted
    }
}

class MainApplication : Application() {

    private val speaker by lazy { SpeakerImpl(this, AnnotationLanguage.RUSSIAN, null) }

    var isSimulationEnabled = false
    var isAvoidTolls = false

    val guidance: Guidance by lazy {
        NaviKitLibrary.createGuidance(
            this,
            PlatformNightModeProviderImpl(),
            NotificationFreedriveDataProviderImpl(applicationContext),
            SoundMuterImpl(),
            null,
            null,
            object : ComplexJunctionsConfigDataProvider {
                override fun data(): ComplexJunctionsConfigData? = null
                override fun setListener(dataProviderListener: ComplexJunctionsConfigDataProviderListener) = Unit
            },
        )
            .apply {
                configurator().apply {
                    setRoadEventsAnnotated(true)
                    setLocalizedSpeaker(speaker, AnnotationLanguage.RUSSIAN)
                    setSpeakerLanguage(AnnotationLanguage.RUSSIAN)
                }
            }
    }

    val consumer: AutomotiveGuidanceConsumer by lazy {
        AutomotiveGuidanceConsumer(
            guidance,
            object : ProjectedStatusProvider {
                override fun isProjectedCreated(): Boolean {
                    return false
                }

                override fun isProjectedResumed(): Boolean {
                    return false
                }

                override fun setListener(listener: ProjectedStatusChangeListener?) {
                }
            },
            GenericGuidanceNotificationManager(
                this,
                guidance
            ).apply {
                setHeadsUpNotificationEnabled(true)
                setNotificationEnabled(true)
            },
            createNotificationBuilder(
                this,
                object : NotificationPendingIntentProvider {
                    override val contentClickPendingIntent: Intent
                        get() = Intent().apply {
                            action = Intent.ACTION_MAIN
                            component =
                                ComponentName(packageName, "ru.yandex.yandexnavi.guidance_lib_test_app.MainActivity")
                        }
                },
                R.string.app_name,
                R.drawable.notifications_navigator_12
            ),
            createNotificationClickReceiver(this),
        )
    }

    override fun attachBaseContext(baseContext: Context) {
        super.attachBaseContext(baseContext)
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()

        MapKitFactory.setApiKey("36614185-aa6d-4547-8ddf-ef1df302b533")
        MapKitFactory.initialize(applicationContext)
        MapKitFactory.getInstance().setStyleType(StyleType.V_NAV2)

        NaviKitLibrary.initRoutePreprocessing(this)
        initializeNotificationChannels(this)

        GenericGuidanceComponent.startService(this)

        NaviKitLibrary.initReporter(
            this,
            object : Metrica {
                override fun report(name: String, params: MutableMap<String, String>) {
                    Log.e(name, params.map { it.key to it.value }.joinToString(", ") { "{${it.first}: ${it.second}}" })
                }

                override fun suspend() = Unit
                override fun resume() = Unit
                override fun setErrorEnvironment(key: String, value: String?) = Unit
            }
        )
    }
}
