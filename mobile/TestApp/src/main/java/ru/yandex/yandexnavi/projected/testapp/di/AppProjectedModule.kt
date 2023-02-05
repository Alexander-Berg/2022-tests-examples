package ru.yandex.yandexnavi.projected.testapp.di

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.widget.Toast
import com.yandex.navikit.projected.ui.road_events.AvailableRoadEventsProvider
import com.yandex.navikit.providers.settings.BooleanSetting
import dagger.Module
import dagger.Provides
import ru.yandex.yandexnavi.projected.platformkit.dependencies.SettingsDelegate
import ru.yandex.yandexnavi.projected.platformkit.dependencies.paywall.PayWallDelegate
import ru.yandex.yandexnavi.projected.platformkit.dependencies.settings.AudioSettingDelegate
import ru.yandex.yandexnavi.projected.platformkit.dependencies.settings.VolumeSettingDelegate
import ru.yandex.yandexnavi.projected.platformkit.di.DebugSettingsAvailabilityProvider
import ru.yandex.yandexnavi.projected.platformkit.di.ProjectedAppAvailabilityProvider
import ru.yandex.yandexnavi.projected.platformkit.lifecycle.ProjectedLifecycleCallbacks
import ru.yandex.yandexnavi.projected.testapp.impl.AudioSettingDelegateImpl
import ru.yandex.yandexnavi.projected.testapp.impl.AvailableRoadEventsProviderImpl
import ru.yandex.yandexnavi.projected.testapp.impl.LifecycleCallbacksImpl
import ru.yandex.yandexnavi.projected.testapp.impl.PayWallDelegateImpl
import ru.yandex.yandexnavi.projected.testapp.impl.StubBooleanSetting
import ru.yandex.yandexnavi.projected.testapp.impl.VolumeSettingDelegateImpl
import ru.yandex.yandexnavi.projected.testapp.ui.MainActivity
import javax.inject.Named

@Module
class AppProjectedModule {
    @Provides
    fun provideStubProvider(context: Context, sp: SharedPreferences): ProjectedAppAvailabilityProvider {
        return object : ProjectedAppAvailabilityProvider {
            override fun isAppDisabled(): Boolean {
                return sp.getBoolean(SHOW_STUB, false)
            }

            override fun onAppDisabledOkClick() {
                Toast.makeText(context, "On stub click", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Provides
    fun provideProjectedLifecycleCallbacks(lifecycle: LifecycleCallbacksImpl): ProjectedLifecycleCallbacks = lifecycle

    @Provides
    fun provideSettingsDelegate(
        @Named("areTrafficJamsEnabled") areTrafficJamsEnabledSetting: BooleanSetting,
        audioSettingDelegate: AudioSettingDelegateImpl,
        volumeSettingDelegate: VolumeSettingDelegateImpl
    ): SettingsDelegate {
        return object : SettingsDelegate {
            override val jamsEnabledSettingDelegate: BooleanSetting
                get() = areTrafficJamsEnabledSetting
            override val audioSettingDelegate: AudioSettingDelegate
                get() = audioSettingDelegate
            override val volumeSettingDelegate: VolumeSettingDelegate
                get() = volumeSettingDelegate
        }
    }

    @Provides
    @Named("areTrafficJamsEnabled")
    fun provideJamEnabledSettingsDelegate(): BooleanSetting {
        return StubBooleanSetting()
    }

    @Provides
    fun providePayWallDelegate(
        context: Context
    ): PayWallDelegate {
        return PayWallDelegateImpl(Intent(context, MainActivity::class.java))
    }

    @Provides
    fun provideAvailableRoadEventsProvider(): AvailableRoadEventsProvider {
        return AvailableRoadEventsProviderImpl()
    }

    @Provides
    fun provideDebugSettingsAvailabilityProvider(): DebugSettingsAvailabilityProvider {
        return object : DebugSettingsAvailabilityProvider {
            override fun isDebugSettingsEnabled(): Boolean {
                return true
            }
        }
    }
}
