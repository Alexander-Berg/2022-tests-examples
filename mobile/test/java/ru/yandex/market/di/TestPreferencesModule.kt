package ru.yandex.market.di

import android.content.Context
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import ru.yandex.market.common.preferences.CommonPreferences
import ru.yandex.market.common.schedulers.WorkerScheduler
import ru.yandex.market.internal.PreferencesDataStore
import ru.yandex.market.internal.rx.preferences.RxSharedPreferences
import javax.inject.Singleton

@Module
class TestPreferencesModule(private val context: Context) {

    @Provides
    fun commonPreferences(): CommonPreferences {
        return CommonPreferences(context.getSharedPreferences("prefs", Context.MODE_PRIVATE))
    }

    @Provides
    fun rxSharedPreferences(commonPreferences: CommonPreferences): RxSharedPreferences {
        return RxSharedPreferences(commonPreferences)
    }

    @Singleton
    @Provides
    fun preferencesDataStore(
        rxSharedPreferences: RxSharedPreferences,
        gson: Gson,
        workerScheduler: WorkerScheduler
    ): PreferencesDataStore {
        return PreferencesDataStore(
            rxSharedPreferences,
            gson,
            workerScheduler
        )
    }
}