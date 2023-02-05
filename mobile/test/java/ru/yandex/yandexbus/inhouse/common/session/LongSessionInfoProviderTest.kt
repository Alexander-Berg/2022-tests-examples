package ru.yandex.yandexbus.inhouse.common.session

import android.app.Activity
import android.os.Bundle
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import ru.yandex.yandexbus.inhouse.BaseTest
import ru.yandex.yandexbus.inhouse.SchedulerProvider
import ru.yandex.yandexbus.inhouse.whenever
import rx.schedulers.Schedulers

class LongSessionInfoProviderTest : BaseTest() {

    @Mock
    private lateinit var mainActivity: Activity

    @Mock
    private lateinit var otherActivity: Activity

    private lateinit var lifecycleNotifier: TestActivityLifecycleNotifier

    private lateinit var sessionInfoStorage: SessionInfoStorage

    private lateinit var longSessionInfoProvider: LongSessionInfoProvider

    private lateinit var schedulerProvider: SchedulerProvider

    @Before
    override fun setUp() {
        super.setUp()

        lifecycleNotifier = TestActivityLifecycleNotifier()
        sessionInfoStorage = TestSessionInfoStorage()
        schedulerProvider = SchedulerProvider(main = Schedulers.immediate(), io = Schedulers.immediate())

        longSessionInfoProvider = LongSessionInfoProvider(
            lifecycleNotifier,
            sessionInfoStorage,
            schedulerProvider
        )
    }

    @Test
    fun `initial session`() {
        val subscriber = longSessionInfoProvider.sessionInfoEvents.test()

        longSessionInfoProvider.watchSessions()

        lifecycleNotifier.activityCreated(mainActivity, null)

        subscriber.assertValue(SessionInfo(sessionNumber = 0L, isActive = true))
    }

    @Test
    fun `subsequent sessions`() {
        whenever(mainActivity.isFinishing).thenReturn(true)

        val subscriber = longSessionInfoProvider.sessionInfoEvents.test()

        longSessionInfoProvider.watchSessions()

        lifecycleNotifier.activityCreated(mainActivity, null)
        lifecycleNotifier.activityDestroyed(mainActivity)

        lifecycleNotifier.activityCreated(mainActivity, null)
        lifecycleNotifier.activityDestroyed(mainActivity)

        subscriber.assertValues(
            SessionInfo(sessionNumber = 0L, isActive = true),
            SessionInfo(sessionNumber = 0L, isActive = false),
            SessionInfo(sessionNumber = 1L, isActive = true),
            SessionInfo(sessionNumber = 1L, isActive = false)
        )
    }

    @Test
    fun `session continues after destroy and not finish`() {
        whenever(mainActivity.isFinishing).thenReturn(false)

        val subscriber = longSessionInfoProvider.sessionInfoEvents.test()

        longSessionInfoProvider.watchSessions()

        lifecycleNotifier.activityCreated(mainActivity, null)
        lifecycleNotifier.activityDestroyed(mainActivity)
        lifecycleNotifier.activityCreated(mainActivity, Bundle())

        subscriber.assertValues(SessionInfo(sessionNumber = 0L, isActive = true))
    }

    @Test
    fun `session persists`() {
        longSessionInfoProvider.watchSessions()
        lifecycleNotifier.activityCreated(mainActivity, null)

        longSessionInfoProvider.sessionInfoEvents
            .test()
            .assertValues(SessionInfo(sessionNumber = 0L, isActive = true))

        longSessionInfoProvider.unwatchSessions()

        val newSessionInfoProvider = LongSessionInfoProvider(
            lifecycleNotifier,
            sessionInfoStorage,
            schedulerProvider
        )

        newSessionInfoProvider.watchSessions()
        lifecycleNotifier.activityCreated(mainActivity, null)

        newSessionInfoProvider.sessionInfoEvents
            .test()
            .assertValues(SessionInfo(sessionNumber = 1L, isActive = true))
    }

    @Test
    fun `activity started and finished atop main doesn't interfere`() {
        val subscriber = longSessionInfoProvider.sessionInfoEvents.test()

        longSessionInfoProvider.watchSessions()

        lifecycleNotifier.activityCreated(mainActivity, null)
        lifecycleNotifier.activityStarted(mainActivity)
        lifecycleNotifier.activityResumed(mainActivity)

        lifecycleNotifier.activityPaused(mainActivity)

        lifecycleNotifier.activityCreated(otherActivity, null)
        lifecycleNotifier.activityStarted(otherActivity)
        lifecycleNotifier.activityResumed(otherActivity)

        lifecycleNotifier.activityStopped(mainActivity)

        whenever(otherActivity.isFinishing).thenReturn(true)

        lifecycleNotifier.activityPaused(otherActivity)

        lifecycleNotifier.activityStarted(mainActivity)
        lifecycleNotifier.activityResumed(mainActivity)

        lifecycleNotifier.activityStopped(otherActivity)
        lifecycleNotifier.activityDestroyed(otherActivity)

        subscriber.assertValue(SessionInfo(sessionNumber = 0L, isActive = true))
    }

    @Test
    fun `launcher activity doesn't interfere`() {
        whenever(otherActivity.isFinishing).thenReturn(true)

        val subscriber = longSessionInfoProvider.sessionInfoEvents.test()

        longSessionInfoProvider.watchSessions()

        lifecycleNotifier.activityCreated(otherActivity, null)
        lifecycleNotifier.activityStarted(otherActivity)
        lifecycleNotifier.activityResumed(otherActivity)

        lifecycleNotifier.activityPaused(otherActivity)

        lifecycleNotifier.activityCreated(mainActivity, null)
        lifecycleNotifier.activityStarted(mainActivity)
        lifecycleNotifier.activityResumed(mainActivity)

        lifecycleNotifier.activityStopped(otherActivity)
        lifecycleNotifier.activityDestroyed(otherActivity)

        subscriber.assertValue(SessionInfo(sessionNumber = 0L, isActive = true))
    }
}

private class TestSessionInfoStorage : SessionInfoStorage {

    private var number: Long? = null

    override fun readAppSessionNumber() = number

    override fun writeAppSessionNumber(number: Long) {
        this.number = number
    }
}
