package ru.auto.feature.panorama.recorder.tf

import io.qameta.allure.kotlin.junit4.AllureRunner
 import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
 import ru.auto.data.prefs.IReactivePrefsDelegate
import ru.auto.data.prefs.MemoryPrefsDelegate
import ru.auto.data.prefs.MemoryReactivePrefsDelegate
import ru.auto.feature.panorama.recorder.tf.PanoramaRecorderRepository.Companion.PREF_IS_PANORAMA_RECORDER_TOOLTIP_HIDDEN

@RunWith(AllureRunner::class) class PanoramaRecorderRepositoryTest {

    private val prefsDelegate = MemoryPrefsDelegate()
    private val prefs: IReactivePrefsDelegate = MemoryReactivePrefsDelegate(prefsDelegate)
    private val panoramaRecorderRepository: IPanoramaRecorderRepository = PanoramaRecorderRepository(prefs)

    @Test
    fun `tooltip should be hidden if in preferences is true`() {
        prefsDelegate.saveBoolean(PREF_IS_PANORAMA_RECORDER_TOOLTIP_HIDDEN, true)
        panoramaRecorderRepository.isTooltipHidden.test().assertValue(true)
    }

    @Test
    fun `tooltip should not be hidden if in preferences is false`() {
        prefsDelegate.saveBoolean(PREF_IS_PANORAMA_RECORDER_TOOLTIP_HIDDEN, false)
        panoramaRecorderRepository.isTooltipHidden.test().assertValue(false)
    }

    @Test
    fun `saveIsTooltipHidden(true) saves true in prefs`() {
        panoramaRecorderRepository.saveIsTooltipHidden(true).test().assertCompleted()
        assertTrue(prefsDelegate.getBoolean(PREF_IS_PANORAMA_RECORDER_TOOLTIP_HIDDEN))
    }

    @Test
    fun `saveIsTooltipHidden(false) saves false in prefs`() {
        panoramaRecorderRepository.saveIsTooltipHidden(false).test().assertCompleted()
        assertFalse(prefsDelegate.getBoolean(PREF_IS_PANORAMA_RECORDER_TOOLTIP_HIDDEN))
    }

}
