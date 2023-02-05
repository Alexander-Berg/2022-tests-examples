package ru.yandex.yandexnavi.projected.testapp.di

import android.content.Context
import android.content.Intent
import dagger.Module
import dagger.Provides
import ru.yandex.yandexnavi.projected.platformkit.domain.repo.ExternalResourceProvider
import ru.yandex.yandexnavi.projected.platformkit.domain.repo.metrica.ProjectedMetricaDelegate
import ru.yandex.yandexnavi.projected.platformkit.domain.repo.protect.IntentToReopenProvider
import ru.yandex.yandexnavi.projected.testapp.R
import ru.yandex.yandexnavi.projected.testapp.impl.ProjectedMetricaDelegateImpl
import ru.yandex.yandexnavi.projected.testapp.ui.MainActivity
import javax.inject.Singleton

private const val SP_NAME = "testSP"
const val SHOW_STUB = "show_stub"

@Module
class AppModule(private val context: Context) {
    @Provides
    fun provideContext(): Context = context

    @Provides
    @Singleton
    fun provideSharedPrefs() = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)

    @Provides
    @Singleton
    fun provideProjectedMetricaDelegate(): ProjectedMetricaDelegate = ProjectedMetricaDelegateImpl()

    @Provides
    @Singleton
    fun intentToReopenProvider(): IntentToReopenProvider {
        return object : IntentToReopenProvider {
            override fun intent(): Intent {
                return Intent(context, MainActivity::class.java)
            }
        }
    }

    @Provides
    @Singleton
    fun providerResources(): ExternalResourceProvider {
        return object : ExternalResourceProvider {
            override fun getNotificationSmallIcon(): Int {
                return R.drawable.notification_arrow
            }
        }
    }
}
