package ru.yandex.yandexbus.inhouse.search.suggest

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import ru.yandex.yandexbus.inhouse.BaseTest
import ru.yandex.yandexbus.inhouse.search.suggest.FollowToMapsFromSearchExperiment.Params
import ru.yandex.yandexbus.inhouse.utils.TimeProvider
import ru.yandex.yandexbus.inhouse.whenever
import java.util.concurrent.TimeUnit

class YMapsBannerExperimentUseCaseTest : BaseTest() {

    @Mock
    private lateinit var experiment: FollowToMapsFromSearchExperiment

    @Mock
    private lateinit var timeProvider: TimeProvider

    private lateinit var prefs: SharedPreferences
    private lateinit var useCase: YMapsSearchBannerExperimentUseCase
    private var currentTime: Long = 0

    @Before
    override fun setUp() {
        super.setUp()
        currentTime = System.currentTimeMillis()
        prefs = context.getSharedPreferences("", Context.MODE_PRIVATE)
        whenever(timeProvider.timeMillis()).thenReturn(currentTime)
        setUpExperiment(active = true)
    }

    @Test
    fun `banner is not shown if experiment is not active`() {
        setUpExperiment(active = false)

        useCase = YMapsSearchBannerExperimentUseCase(experiment, prefs, timeProvider)

        useCase.onScreenOpened()
        assertFalse(useCase.isBannerAllowed)
    }

    @Test
    fun `banner is shown for the first time`() {
        useCase = YMapsSearchBannerExperimentUseCase(experiment, prefs, timeProvider)

        useCase.onScreenOpened()
        assertTrue(useCase.isBannerAllowed)
    }

    @Test
    fun `banner not shown when tapped before`() {
        useCase = YMapsSearchBannerExperimentUseCase(experiment, prefs, timeProvider)

        useCase.onScreenOpened()
        assertTrue(useCase.isBannerAllowed)

        useCase.onBannerTapped()

        for (i in 1..(DEFAULT_DAYS_WITHOUT_BANNER + 1)) {
            advanceTime(DAY)
            useCase.onScreenOpened()
            assertFalse(useCase.isBannerAllowed)
        }
    }

    @Test
    fun `banner shown for specified days count and not shown after`() {
        useCase = YMapsSearchBannerExperimentUseCase(experiment, prefs, timeProvider)

        useCase.onScreenOpened()
        assertTrue(useCase.isBannerAllowed) //First "day"

        advanceTime(DAY)
        useCase.onScreenOpened()
        assertTrue(useCase.isBannerAllowed) //Second "day"

        advanceTime(DAY)
        useCase.onScreenOpened()
        assertFalse(useCase.isBannerAllowed)
    }

    @Test
    fun `banner not shown after been expired 2 times`() {
        useCase = YMapsSearchBannerExperimentUseCase(experiment, prefs, timeProvider)

        useCase.onScreenOpened()
        assertTrue(useCase.isBannerAllowed) //First "day"

        advanceTime(DAY)
        useCase.onScreenOpened()
        assertTrue(useCase.isBannerAllowed) //Second "day"

        for (i in 1..(DEFAULT_DAYS_WITHOUT_BANNER)) {
            advanceTime(DAY)
            useCase.onScreenOpened()
            assertFalse(useCase.isBannerAllowed)
        }

        advanceTime(DAY)
        useCase.onScreenOpened()
        assertTrue(useCase.isBannerAllowed) //First "day"

        advanceTime(DAY)
        useCase.onScreenOpened()
        assertTrue(useCase.isBannerAllowed) //Second "day"


        for (i in 1..DEFAULT_DAYS_WITHOUT_BANNER + 1) {  //closed twice, not allowed anymore
            advanceTime(DAY)
            useCase.onScreenOpened()
            assertFalse(useCase.isBannerAllowed)
        }
    }

    @Test
    fun `banner not shown after been manually closed 2 times`() {
        useCase = YMapsSearchBannerExperimentUseCase(experiment, prefs, timeProvider)

        useCase.onScreenOpened()
        assertTrue(useCase.isBannerAllowed)

        useCase.onBannerClosed()
        assertFalse(useCase.isBannerAllowed) // first "day without banner"

        for (i in 1..(DEFAULT_DAYS_WITHOUT_BANNER - 1)) {
            advanceTime(DAY)
            useCase.onScreenOpened()
            assertFalse(useCase.isBannerAllowed)
        }

        advanceTime(DAY)
        useCase.onScreenOpened()
        assertTrue(useCase.isBannerAllowed)

        useCase.onBannerClosed()
        assertFalse(useCase.isBannerAllowed) // first "day without banner"

        for (i in 1..(DEFAULT_DAYS_WITHOUT_BANNER - 1)) {
            advanceTime(DAY)
            useCase.onScreenOpened()
            assertFalse(useCase.isBannerAllowed)
        }

        advanceTime(DAY)
        useCase.onScreenOpened()
        assertFalse(useCase.isBannerAllowed) //closed twice, not allowed anymore
    }

    @Test
    fun `banner not shown after been tapped`() {
        useCase = YMapsSearchBannerExperimentUseCase(experiment, prefs, timeProvider)

        useCase.onScreenOpened()
        assertTrue(useCase.isBannerAllowed)

        useCase.onBannerTapped()
        assertFalse(useCase.isBannerAllowed)

        for (i in 1..(DEFAULT_DAYS_WITHOUT_BANNER + 1)) {
            advanceTime(DAY)
            useCase.onScreenOpened()
            assertFalse(useCase.isBannerAllowed)
        }
    }

    @Test
    fun `closed banner not shown for specified days count and shown after`() {
        useCase = YMapsSearchBannerExperimentUseCase(experiment, prefs, timeProvider)

        useCase.onScreenOpened()
        assertTrue(useCase.isBannerAllowed)

        useCase.onBannerClosed()
        assertFalse(useCase.isBannerAllowed)

        advanceTime(DAY)
        useCase.onScreenOpened()
        assertFalse(useCase.isBannerAllowed)

        advanceTime(DAY)
        useCase.onScreenOpened()
        assertTrue(useCase.isBannerAllowed)
    }

    private fun setUpExperiment(active: Boolean) {
        whenever(experiment.getParams()).thenReturn(
            if (active) Params(DEFAULT_DAYS_WITH_BANNER, DEFAULT_DAYS_WITHOUT_BANNER) else null
        )
    }

    private fun advanceTime(millis: Long) {
        currentTime += millis
        whenever(timeProvider.timeMillis()).thenReturn(currentTime)
    }

    private companion object {
        private val DAY = TimeUnit.DAYS.toMillis(1)
        private const val DEFAULT_DAYS_WITH_BANNER = 2
        private const val DEFAULT_DAYS_WITHOUT_BANNER = 2
    }
}

