package ru.yandex.yandexnavi.core

import android.app.Activity
import android.app.Application
import com.yandex.navi.NavilibLifecycle
import com.yandex.navikit.report.Metrica
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import ru.yandex.yandexnavi.core.lifecycle.NavigatorLifecycle
import ru.yandex.yandexnavi.projected.platformkit.lifecycle.ProjectedLifecycleCallbacks

@Suppress("TestFunctionName")
class NavigatorLifecycleTest {
    private val activity = Activity()
    private lateinit var callbacks: Callbacks
    private lateinit var activityLC: Application.ActivityLifecycleCallbacks
    private lateinit var projectedLC: ProjectedLifecycleCallbacks

    @Before
    fun initTests() {
        callbacks = Callbacks()
        val lifecycle = NavigatorLifecycle(
            callbacks,
            object : Metrica {
                override fun report(name: String, params: MutableMap<String, String>) {
                }

                override fun resume() {
                }

                override fun suspend() {
                }

                override fun setErrorEnvironment(key: String, value: String?) {
                }
            }
        )

        activityLC = lifecycle.activityLifecycle
        projectedLC = lifecycle.projectedLifecycle
    }

    @Test
    fun testOnlyActivityLC() {
        NOT_INIT()

        activityLC.onActivityCreated(activity, null)
        CREATED()
        activityLC.onActivityStarted(activity)
        STARTED()
        activityLC.onActivityResumed(activity)
        RESUMED()

        activityLC.onActivityPaused(activity)
        STARTED()
        activityLC.onActivityStopped(activity)
        CREATED()
        activityLC.onActivityDestroyed(activity)
        CREATED()
    }

    @Test
    fun testOpenTwoActivityLC() {
        val activity1 = Activity()
        val activity2 = Activity()
        NOT_INIT()

        // open first activity
        activityLC.onActivityCreated(activity1, null)
        CREATED()
        activityLC.onActivityStarted(activity1)
        STARTED()
        activityLC.onActivityResumed(activity1)
        RESUMED()

        // open second activity, first activity is in background
        activityLC.onActivityPaused(activity1)
        STARTED()
        activityLC.onActivityStopped(activity1)
        CREATED()

        activityLC.onActivityCreated(activity2, null)
        CREATED()
        activityLC.onActivityStarted(activity2)
        STARTED()
        activityLC.onActivityResumed(activity2)
        RESUMED()

        // close second activity, first activity is in foreground
        activityLC.onActivityPaused(activity2)
        STARTED()
        activityLC.onActivityStopped(activity2)
        CREATED()
        activityLC.onActivityDestroyed(activity2)
        CREATED()

        activityLC.onActivityStarted(activity1)
        STARTED()
        activityLC.onActivityResumed(activity1)
        RESUMED()

        // close first activity
        activityLC.onActivityPaused(activity1)
        STARTED()
        activityLC.onActivityStopped(activity1)
        CREATED()
        activityLC.onActivityDestroyed(activity1)
        CREATED()
    }

    @Test
    fun testOnlyProjectedLC() {
        NOT_INIT()

        projectedLC.onProjectedCreate()
        CREATED()
        projectedLC.onProjectedStart()
        STARTED()
        projectedLC.onProjectedResume()
        RESUMED()

        projectedLC.onProjectedPause()
        STARTED()
        projectedLC.onProjectedStop()
        CREATED()
        projectedLC.onProjectedDestroy()
        CREATED()
    }

    @Test
    fun testOpenActivityWhileProjectedIsCreating() {
        projectedLC.onProjectedCreate()
        CREATED()

        // start and close activity
        activityLC.onActivityCreated(activity, null)
        activityLC.onActivityStarted(activity)
        activityLC.onActivityResumed(activity)
        RESUMED()
        activityLC.onActivityPaused(activity)
        activityLC.onActivityStopped(activity)
        activityLC.onActivityDestroyed(activity)
        CREATED()

        projectedLC.onProjectedStart()
        STARTED()

        // start and close activity
        activityLC.onActivityCreated(activity, null)
        activityLC.onActivityStarted(activity)
        activityLC.onActivityResumed(activity)
        RESUMED()
        activityLC.onActivityPaused(activity)
        activityLC.onActivityStopped(activity)
        activityLC.onActivityDestroyed(activity)
        STARTED()

        projectedLC.onProjectedResume()
        RESUMED()

        // start and close activity
        activityLC.onActivityCreated(activity, null)
        activityLC.onActivityStarted(activity)
        activityLC.onActivityResumed(activity)
        RESUMED()
        activityLC.onActivityPaused(activity)
        activityLC.onActivityStopped(activity)
        activityLC.onActivityDestroyed(activity)
        RESUMED()

        projectedLC.onProjectedPause()
        STARTED()

        // start and close activity
        activityLC.onActivityCreated(activity, null)
        activityLC.onActivityStarted(activity)
        activityLC.onActivityResumed(activity)
        RESUMED()
        activityLC.onActivityPaused(activity)
        activityLC.onActivityStopped(activity)
        activityLC.onActivityDestroyed(activity)
        STARTED()

        projectedLC.onProjectedStop()
        CREATED()

        // start and close activity
        activityLC.onActivityCreated(activity, null)
        activityLC.onActivityStarted(activity)
        activityLC.onActivityResumed(activity)
        RESUMED()
        activityLC.onActivityPaused(activity)
        activityLC.onActivityStopped(activity)
        activityLC.onActivityDestroyed(activity)
        CREATED()
    }

    @Test
    fun testOpenProjectedWhileActivityIsCreating() {
        activityLC.onActivityCreated(activity, null)
        activityLC.onActivityStarted(activity)
        activityLC.onActivityResumed(activity)
        RESUMED()

        // open projected
        projectedLC.onProjectedCreate()
        RESUMED()
        projectedLC.onProjectedStart()
        RESUMED()
        projectedLC.onProjectedResume()
        RESUMED()

        // close activity
        activityLC.onActivityPaused(activity)
        RESUMED()
        activityLC.onActivityStopped(activity)
        RESUMED()
        activityLC.onActivityDestroyed(activity)
        RESUMED()

        // close projected
        projectedLC.onProjectedPause()
        STARTED()
        projectedLC.onProjectedStop()
        CREATED()
        projectedLC.onProjectedDestroy()
        CREATED()

        // open activity again
        activityLC.onActivityCreated(activity, null)
        CREATED()
        activityLC.onActivityStarted(activity)
        STARTED()
        activityLC.onActivityResumed(activity)
        RESUMED()
    }

    @Test
    fun testOpenAndHideProjectedWhileActivityIsOpened() {
        activityLC.onActivityCreated(activity, null)
        activityLC.onActivityStarted(activity)
        activityLC.onActivityResumed(activity)
        RESUMED()

        projectedLC.onProjectedCreate()
        RESUMED()
        projectedLC.onProjectedStart()
        RESUMED()
        projectedLC.onProjectedResume()
        RESUMED()

        // projected in background
        // resumed cause activity is open
        projectedLC.onProjectedPause()
        RESUMED()
        projectedLC.onProjectedStop()
        RESUMED()

        // projected in foreground
        projectedLC.onProjectedStart()
        RESUMED()
        projectedLC.onProjectedResume()
        RESUMED()

        // close projected
        projectedLC.onProjectedPause()
        RESUMED()
        projectedLC.onProjectedStop()
        RESUMED()
        projectedLC.onProjectedDestroy()
        // RESUMED because activity is still opened
        RESUMED()

        // close activity
        activityLC.onActivityPaused(activity)
        STARTED()
        activityLC.onActivityStopped(activity)
        CREATED()
        activityLC.onActivityDestroyed(activity)
        CREATED()
    }

    @Test
    fun testActivityLCAfterProjectedDestroyed() {
        // open and close projected
        projectedLC.onProjectedCreate()
        projectedLC.onProjectedStart()
        projectedLC.onProjectedResume()
        RESUMED()
        projectedLC.onProjectedPause()
        projectedLC.onProjectedStop()
        projectedLC.onProjectedDestroy()
        CREATED()

        // activity LC must count if projected closed
        activityLC.onActivityCreated(activity, null)
        CREATED()
        activityLC.onActivityStarted(activity)
        STARTED()
        activityLC.onActivityResumed(activity)
        RESUMED()
        activityLC.onActivityPaused(activity)
        STARTED()
        activityLC.onActivityStopped(activity)
        CREATED()
        activityLC.onActivityDestroyed(activity)
        CREATED()
    }

    class Callbacks : NavilibLifecycle {
        val counter = Counter(0, 0, 0)

        override fun onCreate() {
            counter.created++
        }

        override fun onStart() {
            counter.started++
        }

        override fun onResume() {
            counter.resumed++
        }

        override fun onPause() {
            counter.resumed--
        }

        override fun onStop() {
            counter.started--
        }
    }

    // use int (not boolean) to detect 2 calls in a row
    data class Counter(
        // created counter must be exactly 1 after any calls
        var created: Int,
        var started: Int,
        var resumed: Int
    )

    private fun NOT_INIT() {
        assertEquals(Counter(0, 0, 0), callbacks.counter)
    }

    private fun CREATED() {
        assertEquals(Counter(1, 0, 0), callbacks.counter)
    }

    private fun STARTED() {
        assertEquals(Counter(1, 1, 0), callbacks.counter)
    }

    private fun RESUMED() {
        assertEquals(Counter(1, 1, 1), callbacks.counter)
    }
}
