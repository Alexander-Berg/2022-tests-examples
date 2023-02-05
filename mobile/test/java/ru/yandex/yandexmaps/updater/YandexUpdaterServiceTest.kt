package ru.yandex.yandexmaps.updater

import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import com.yandex.updater.lib.Updater
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import ru.yandex.yandexmaps.common.preferences.PreferencesFactory
import ru.yandex.yandexmaps.multiplatform.debug.panel.api.ExperimentManager
import ru.yandex.yandexmaps.multiplatform.debug.panel.experiments.KnownExperiments
import utils.TestActivity
import utils.mock
import utils.shouldReturn
import javax.inject.Provider

@RunWith(RobolectricTestRunner::class)
class YandexUpdaterServiceTest {

    private lateinit var activityController: ActivityController<TestActivity>
    private lateinit var updater: Updater
    private lateinit var experimentManager: ExperimentManager
    private lateinit var preferencesFactory: PreferencesFactory

    @Before
    fun before() {
        activityController = Robolectric.buildActivity(TestActivity::class.java).setup()
        updater = mock()
        experimentManager = mock()
        preferencesFactory = mock()
    }

    @Test
    fun `do not call updater if experiment switches off`() {
        val updaterProvider = mock<Provider<Updater>>()
        experimentManager.get(KnownExperiments.yandexUpdaterBaseUrl).shouldReturn("")

        activityController
            .get()
            .let { activity ->
                YandexUpdaterService(
                    activity,
                    updaterProvider,
                    { experimentManager },
                    preferencesFactory,
                )

                verify(updaterProvider, never()).get()
            }
    }
}
