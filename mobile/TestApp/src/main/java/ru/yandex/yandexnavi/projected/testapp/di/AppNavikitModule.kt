package ru.yandex.yandexnavi.projected.testapp.di

import SpeakerImpl
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.yandex.datasync.DatabaseManagerFactory
import com.yandex.mapkit.annotations.AnnotationLanguage
import com.yandex.mapkit.annotations.Speaker
import com.yandex.mapkit.directions.DirectionsFactory
import com.yandex.mapkit.directions.driving.DrivingRouter
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.mapview.MapSurface
import com.yandex.navikit.NaviKitLibrary
import com.yandex.navikit.RouteSuggest
import com.yandex.navikit.RouteSuggestProvider
import com.yandex.navikit.auth.NavikitAccount
import com.yandex.navikit.auth.PasswordRequiredHandler
import com.yandex.navikit.destination_suggest.DestinationSuggestManager
import com.yandex.navikit.destination_suggest.StatisticalModel
import com.yandex.navikit.guidance.Guidance
import com.yandex.navikit.guidance.SoundMuter
import com.yandex.navikit.guidance.automotive.AutomotiveGuidanceConsumer
import com.yandex.navikit.guidance.automotive.ProjectedStatusChangeListener
import com.yandex.navikit.guidance.automotive.ProjectedStatusProvider
import com.yandex.navikit.guidance.automotive.notification.AutomotiveGuidanceNotificationBuilder
import com.yandex.navikit.guidance.notification.GenericGuidanceNotificationManager
import com.yandex.navikit.night_mode.NativeNightModeListener
import com.yandex.navikit.night_mode.PlatformNightModeProvider
import com.yandex.navikit.notifications.NotificationFreedriveDataProvider
import com.yandex.navikit.projected.ui.AnnotationsPlayer
import com.yandex.navikit.providers.bookmarks.BookmarksProvider
import com.yandex.navikit.providers.camera_transform_storage.CameraTransform
import com.yandex.navikit.providers.camera_transform_storage.PlatformCameraTransformStorage
import com.yandex.navikit.providers.places.PlacesProvider
import com.yandex.navikit.ride_history.RideType
import com.yandex.navikit.ride_history.RideTypeProvider
import dagger.Module
import dagger.Provides
import ru.yandex.yandexnavi.projected.platformkit.ProjectedKit
import ru.yandex.yandexnavi.projected.platformkit.dependencies.SearchApi
import ru.yandex.yandexnavi.projected.platformkit.dependencies.SearchApiFactory
import ru.yandex.yandexnavi.projected.platformkit.notification.AndroidAutoNotificationDecorator
import ru.yandex.yandexnavi.projected.platformkit.notification.AndroidAutoNotificationFreedriveDataProvider
import ru.yandex.yandexnavi.projected.platformkit.presentation.search.SearchCameraController
import ru.yandex.yandexnavi.projected.testapp.R
import ru.yandex.yandexnavi.projected.testapp.impl.BookmarksProviderImpl
import ru.yandex.yandexnavi.projected.testapp.impl.PlacesProviderImpl
import ru.yandex.yandexnavi.projected.testapp.impl.SearchApiImpl
import ru.yandex.yandexnavi.projected.testapp.impl.SoundMuterImpl
import ru.yandex.yandexnavi.ui.guidance.notifications.NotificationPendingIntentProvider
import ru.yandex.yandexnavi.ui.guidance.notifications.createNotificationBuilder
import ru.yandex.yandexnavi.ui.guidance.notifications.createNotificationClickReceiver
import javax.inject.Singleton

@Module
class AppNavikitModule {
    @Singleton
    @Provides
    fun provideNightMode(): PlatformNightModeProvider {
        return object : PlatformNightModeProvider {
            override fun bindListener(nightModeListener: NativeNightModeListener) = Unit
            override fun isNightMode(): Boolean? = true
        }
    }

    @Singleton
    @Provides
    fun provideSpeaker(context: Context): Speaker {
        return SpeakerImpl(context, AnnotationLanguage.RUSSIAN, null)
    }

    @Singleton
    @Provides
    fun provideNotificationBuilder(
        context: Context
    ): AutomotiveGuidanceNotificationBuilder {
        return createNotificationBuilder(
            context,
            object : NotificationPendingIntentProvider {
                override val contentClickPendingIntent: Intent
                    get() = Intent().apply {
                        action = Intent.ACTION_MAIN
                        component =
                            ComponentName(context.packageName, "ru.yandex.yandexnavi.projected.testapp.ui.MainActivity")
                    }
            },
            R.string.app_name,
            R.drawable.temp_notification_icon,
            AndroidAutoNotificationDecorator(context, ProjectedKit.getNotificationCustomizationDelegate())
        )
    }

    @Singleton
    @Provides
    fun provideSoundMuter(): SoundMuter {
        return SoundMuterImpl()
    }

    @Singleton
    @Provides
    fun provideFreeDriveData(): NotificationFreedriveDataProvider {
        return AndroidAutoNotificationFreedriveDataProvider(null, ProjectedKit.getNotificationCustomizationDelegate())
    }

    @Singleton
    @Provides
    fun provideGuidance(
        context: Context,
        speaker: Speaker,
        nightModeProvider: PlatformNightModeProvider,
        freedriveDataProvider: NotificationFreedriveDataProvider,
        soundMuter: SoundMuter
    ): Guidance {
        return NaviKitLibrary.createGuidance(
            context, nightModeProvider, freedriveDataProvider,
            soundMuter, null, null, null
        )
            .apply {
                configurator().apply {
                    setLocalizedSpeaker(speaker, AnnotationLanguage.RUSSIAN)
                    setSpeakerLanguage(AnnotationLanguage.RUSSIAN)
                }
            }
    }

    @Singleton
    @Provides
    fun provideProjectedStatusProvider(): ProjectedStatusProvider {
        return object : ProjectedStatusProvider {
            override fun isProjectedCreated(): Boolean {
                return false
            }

            override fun isProjectedResumed(): Boolean {
                return false
            }

            override fun setListener(listener: ProjectedStatusChangeListener?) {
            }
        }
    }

    @Singleton
    @Provides
    fun provideNotificationManager(
        context: Context,
        guidance: Guidance,
    ): GenericGuidanceNotificationManager {
        return GenericGuidanceNotificationManager(
            context,
            guidance
        ).apply {
            setHeadsUpNotificationEnabled(true)
            setNotificationEnabled(true)
        }
    }

    @Singleton
    @Provides
    fun provideConsumer(
        context: Context,
        guidance: Guidance,
        projected: ProjectedStatusProvider,
        builder: AutomotiveGuidanceNotificationBuilder,
        manager: GenericGuidanceNotificationManager,
    ): AutomotiveGuidanceConsumer {
        return AutomotiveGuidanceConsumer(
            guidance,
            projected,
            manager,
            builder,
            createNotificationClickReceiver(context),
        )
    }

    @Singleton
    @Provides
    fun provideBookmarksProvider(): BookmarksProvider {
        return BookmarksProviderImpl()
    }

    @Singleton
    @Provides
    fun providePlacesProvider(): PlacesProvider {
        return PlacesProviderImpl()
    }

    @Singleton
    @Provides
    fun provideRouter(): DrivingRouter {
        return DirectionsFactory.getInstance().createDrivingRouter()
    }

    @Singleton
    @Provides
    fun provideSuggestProvider(
        bookmarksProvider: BookmarksProvider,
        placesProvider: PlacesProvider
    ): RouteSuggestProvider {
        return object : RouteSuggestProvider {
            override fun passwordRequiredHandler(): PasswordRequiredHandler {
                return PasswordRequiredHandler { }
            }

            override fun bookmarksProvider(): BookmarksProvider {
                return bookmarksProvider
            }

            override fun statisticalModel(): StatisticalModel {
                return StatisticalModel.NAVI
            }

            override fun isDrivingSummaryEnabled(): Boolean {
                return true
            }

            override fun rideTypeProvider(): RideTypeProvider {
                return RideTypeProvider { RideType.CAR }
            }

            override fun authAccount(): NavikitAccount? {
                return null
            }

            override fun placesProvider(): PlacesProvider {
                return placesProvider
            }
        }
    }

    @Singleton
    @Provides
    fun provideRouteSuggest(context: Context, provider: RouteSuggestProvider, guidance: Guidance): RouteSuggest {
        return NaviKitLibrary.createRouteSuggest(context, provider, guidance, DatabaseManagerFactory.getInstance())
    }

    @Singleton
    @Provides
    fun provideSearch(): SearchApiFactory {
        return object : SearchApiFactory {
            override fun create(
                carContext: Context,
                mapSurface: MapSurface,
                searchCameraController: SearchCameraController,
                pageSize: Int?
            ): SearchApi {
                return SearchApiImpl()
            }
        }
    }

    @Singleton
    @Provides
    fun provideSuggest(suggest: RouteSuggest): DestinationSuggestManager = suggest.destinationSuggestManager()

    @Singleton
    @Provides
    fun provideAnnotationsPlayer(): AnnotationsPlayer {
        return object : AnnotationsPlayer {
            override fun playRouteBuiltAnnotation() = Unit
        }
    }

    @Singleton
    @Provides
    fun provideCameraTransformStorage(): PlatformCameraTransformStorage {
        return object : PlatformCameraTransformStorage {
            private var cameraTransform_ = CameraTransform(Point(55.735520, 37.642474), 12.0f)
            override fun cameraTransform(): CameraTransform = cameraTransform_

            override fun setCameraTransform(value: CameraTransform) {
                cameraTransform_ = value
            }
        }
    }
}
