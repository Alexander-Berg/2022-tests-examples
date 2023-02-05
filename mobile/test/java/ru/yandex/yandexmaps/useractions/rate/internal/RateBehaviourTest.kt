package ru.yandex.yandexmaps.useractions.rate.internal

import android.app.Application
import io.reactivex.schedulers.TestScheduler
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import ru.yandex.yandexmaps.multiplatform.analytics.GeneratedAppAnalytics
import ru.yandex.yandexmaps.useractions.rate.api.RateAgainExperiment
import ru.yandex.yandexmaps.useractions.rate.api.RateExperimentProbability
import ru.yandex.yandexmaps.useractions.rate.api.ShowStatus.ALREADY_RATED
import ru.yandex.yandexmaps.useractions.rate.api.ShowStatus.NOT_LUCKY
import ru.yandex.yandexmaps.useractions.rate.api.ShowStatus.SHOWN_2_TIMES
import ru.yandex.yandexmaps.useractions.rate.api.ShowStatus.SHOW_NOW
import ru.yandex.yandexmaps.useractions.rate.api.ShowStatus.WAIT_FIRST_PERIOD
import ru.yandex.yandexmaps.useractions.rate.api.ShowStatus.WAIT_SECOND_PERIOD
import ru.yandex.yandexmaps.useractions.rate.api.ShowStatus.WAIT_SIGNIFICANT_COUNTER
import ru.yandex.yandexmaps.useractions.rate.api.ShowStatus.WAIT_TO_SHOW_AGAIN
import ru.yandex.yandexmaps.useractions.rate.internal.RateContants.EVENTS_COUNT_TO_SHOW
import ru.yandex.yandexmaps.useractions.rate.internal.RateContants.FIRST_SHOW_DELAY
import ru.yandex.yandexmaps.useractions.rate.internal.RateContants.SECOND_SHOW_DELAY
import ru.yandex.yandexmaps.useractions.rate.internal.RateContants.SHOW_AGAIN_DELAY_IN_MONTHS
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import java.util.Calendar
import java.util.concurrent.TimeUnit

class RateBehaviourTest {

    private val scheduler = TestScheduler()

    @Mock private lateinit var app: Application

    private lateinit var storage: RateDataStorage
    private lateinit var rateBehaviour: RateBehaviour

    private lateinit var mocksCloseable: AutoCloseable

    @Before
    fun setUp() {
        mocksCloseable = MockitoAnnotations.openMocks(this)
        app = mock(Application::class.java)
        storage = emptyStorage()
        rateBehaviour = RateBehaviour(app, alwaysShowRateExperimentProbability, rateAgainExperiment, storage, scheduler)
    }

    @After
    fun tearDown() {
        mocksCloseable.close()
    }

    @Test
    fun `rate experiment probability - always`() {
        rateBehaviour.userRatedApp()
        expectThat(rateBehaviour.canShowNow()).isEqualTo(ALREADY_RATED)
    }

    @Test
    fun `rate again experiment enabled not rated but waiting 4 months`() {
        rateBehaviour = RateBehaviour(
            app, alwaysShowRateExperimentProbability,
            object : RateAgainExperiment {
                override fun invoke(): Boolean = true
            },
            storage, scheduler
        )
        storage.eventCount = EVENTS_COUNT_TO_SHOW
        storage.showCount = 2
        expectThat(rateBehaviour.canShowNow()).isEqualTo(WAIT_TO_SHOW_AGAIN)
    }

    @Test
    fun `rate again experiment enabled not rated show after 4 month`() {
        rateBehaviour = RateBehaviour(
            app, alwaysShowRateExperimentProbability,
            object : RateAgainExperiment {
                override fun invoke(): Boolean = true
            },
            storage, scheduler
        )
        storage.eventCount = EVENTS_COUNT_TO_SHOW
        storage.lastShowTime = System.currentTimeMillis()
        val showAgainDelay = Calendar.getInstance().apply {
            timeInMillis = storage.lastShowTime
            add(Calendar.MONTH, SHOW_AGAIN_DELAY_IN_MONTHS)
        }.timeInMillis
        storage.showCount = 2
        scheduler.advanceTimeBy(showAgainDelay, TimeUnit.MILLISECONDS)
        expectThat(rateBehaviour.canShowNow()).isEqualTo(SHOW_NOW)
    }

    @Test
    fun `rate again experiment disabled shown twice already`() {
        repeat(EVENTS_COUNT_TO_SHOW + 1) {
            rateBehaviour.eventHappened()
        }
        storage.showCount = 2
        expectThat(rateBehaviour.canShowNow()).isEqualTo(SHOWN_2_TIMES)
    }

    @Test
    fun `rate is saved in storage`() {
        expectThat(storage.rated).isFalse()
        rateBehaviour.userRatedApp()
        expectThat(storage.rated).isTrue()
    }

    @Test
    fun `wait second show rate period`() {
        storage.lastShowTime = System.currentTimeMillis()
        storage.showCount = 1
        storage.eventCount = EVENTS_COUNT_TO_SHOW
        scheduler.advanceTimeBy(storage.lastShowTime, TimeUnit.MILLISECONDS)
        expectThat(rateBehaviour.canShowNow()).isEqualTo(WAIT_SECOND_PERIOD)
    }

    @Test
    fun `show leads to showSaving eventsCountResetting firstShowSetting`() {
        scheduler.advanceTimeBy(0xdead_beefL, TimeUnit.MILLISECONDS)

        rateBehaviour.dialogShown(GeneratedAppAnalytics.ApplicationShowRateMeAlertTrigger.FEEDBACK_CLOSE)

        expectThat(storage.showCount).isEqualTo(1)
        expectThat(storage.eventCount).isEqualTo(0)
        expectThat(storage.lastShowTime).isEqualTo(0xdead_beefL)

        rateBehaviour.dialogShown(GeneratedAppAnalytics.ApplicationShowRateMeAlertTrigger.FEEDBACK_CLOSE)
        expectThat(storage.showCount).isEqualTo(2)
        expectThat(storage.eventCount).isEqualTo(0)
        expectThat(storage.lastShowTime).isEqualTo(0xdead_beefL)
    }

    @Test
    fun `event happening is saved into storage`() {
        expectThat(storage.eventCount).isEqualTo(0)
        rateBehaviour.eventHappened()
        expectThat(storage.eventCount).isEqualTo(1)
    }

    @Test
    fun `can not rate if already rated`() {
        repeat(EVENTS_COUNT_TO_SHOW) { rateBehaviour.eventHappened() }
        rateBehaviour.userRatedApp()
        expectThat(rateBehaviour.canShowNow()).isEqualTo(ALREADY_RATED)
    }

    @Test
    fun `cant rate if significant events count is not reached`() {
        repeat(EVENTS_COUNT_TO_SHOW - 1) { iteration ->
            rateBehaviour.eventHappened()
            expectThat(rateBehaviour.canShowNow())
                .describedAs("Failed on iteration# $iteration")
                .isEqualTo(WAIT_SIGNIFICANT_COUNTER)
        }
    }

    @Test
    fun `cant rate if first show date is not reached`() {
        repeat(EVENTS_COUNT_TO_SHOW) { rateBehaviour.eventHappened() }
        expectThat(rateBehaviour.canShowNow()).isEqualTo(WAIT_FIRST_PERIOD)
    }

    @Test
    fun `app could be rated first time`() {
        scheduler.advanceTimeBy(FIRST_SHOW_DELAY + 1, TimeUnit.MILLISECONDS)
        repeat(EVENTS_COUNT_TO_SHOW) { rateBehaviour.eventHappened() }
        expectThat(rateBehaviour.canShowNow()).isEqualTo(SHOW_NOW)
    }

    @Test
    fun `app could be rated second time`() {
        storage.showCount = 1
        storage.eventCount = EVENTS_COUNT_TO_SHOW

        scheduler.advanceTimeBy(FIRST_SHOW_DELAY + SECOND_SHOW_DELAY + 1, TimeUnit.MILLISECONDS)
        expectThat(rateBehaviour.canShowNow()).isEqualTo(SHOW_NOW)
    }

    @Test
    fun `rate experiment probability to null`() {
        scheduler.advanceTimeBy(FIRST_SHOW_DELAY + 1, TimeUnit.MILLISECONDS)
        repeat(EVENTS_COUNT_TO_SHOW) { rateBehaviour.eventHappened() }
        rateBehaviour = RateBehaviour(
            app,
            object : RateExperimentProbability {
                override fun invoke(): Int? = null
            },
            rateAgainExperiment, storage, scheduler
        )
        expectThat(rateBehaviour.canShowNow()).isEqualTo(NOT_LUCKY)
    }

    private val alwaysShowRateExperimentProbability = object : RateExperimentProbability {
        override fun invoke(): Int = 100
    }

    private val rateAgainExperiment = object : RateAgainExperiment {
        override fun invoke(): Boolean = false
    }

    private fun emptyStorage(): RateDataStorage = object : RateDataStorage {
        override var appVersionCode = 0L
        override var lastShowTime = 0L
        override var showCount = 0
        override var firstLaunchDate = 0L
        override var eventCount = 0
        override var rated = false
    }
}
