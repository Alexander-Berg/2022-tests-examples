package ru.yandex.yandexmaps.app

import android.content.Context
import ru.yandex.maps.appkit.common.Preferences.LANGUAGE
import ru.yandex.yandexmaps.app.di.components.DaggerTestApplicationComponent
import ru.yandex.yandexmaps.app.di.components.TestApplicationComponent
import ru.yandex.yandexmaps.common.app.Language

class TestMapsApplication : MapsApplication() {

    companion object {
        fun get(context: Context): TestMapsApplication {
            return context.applicationContext as TestMapsApplication
        }
    }

    private var initialExperiments: Map<String, String?> = emptyMap()
    private var initialApplicationComponent = true

    fun setInitialExperiments(initialExperiments: Map<String, String?>) {
        this.initialExperiments = initialExperiments
        initialApplicationComponent = true
    }

    override fun applicationComponent(): TestApplicationComponent {
        if (applicationComponent == null || initialApplicationComponent) {
            initialApplicationComponent = false
            preferences.put(LANGUAGE, Language.RU, commitSynchronously = true)
            applicationComponent = DaggerTestApplicationComponent.builder()
                .bindApplication(this)
                .bindPreferences(preferences)
                .initialExperiments(initialExperiments)
                .build()
        }
        return applicationComponent as TestApplicationComponent
    }
}
