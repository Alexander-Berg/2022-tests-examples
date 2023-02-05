package com.edadeal.android.data

import android.app.Activity
import com.edadeal.android.AndroidLocation
import com.edadeal.android.dto.Analytics
import com.edadeal.android.metrics.AdjustKit
import com.edadeal.android.metrics.Metrics
import com.nhaarman.mockito_kotlin.mock
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class AnalyticsRepositoryTest(
    private val defaults: List<Analytics.Analytic>,
    private val analytics: List<Analytics.Analytic>,
    private val expected: List<Pair<Metrics.Collector, Int>>
) {

    companion object {
        private val log = Analytics.Analytic(name = Analytics.nameLog, level = Analytics.levelAll)
        private val appmetrika = Analytics.Analytic(name = Analytics.nameYandex, level = Analytics.levelAll)
        private val crashlytics = Analytics.Analytic(name = Analytics.nameCrashlytics, level = Analytics.levelOff)

        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<Any>> = listOf(
            arrayOf<Any>(
                emptyList<Any>(),
                listOf(appmetrika),
                listOf(
                    Collector(Analytics.nameYandex) to Analytics.levelAll,
                    Collector(Analytics.nameCrashlytics) to Analytics.levelOff
                )
            ),
            arrayOf<Any>(
                listOf(log, appmetrika),
                listOf(appmetrika, crashlytics),
                listOf(
                    Collector(Analytics.nameLog) to Analytics.levelAll,
                    Collector(Analytics.nameYandex) to Analytics.levelAll,
                    Collector(Analytics.nameCrashlytics) to Analytics.levelOff
                )
            ),
            arrayOf<Any>(
                listOf(appmetrika, crashlytics),
                listOf(crashlytics.copy(level = Analytics.levelAll), appmetrika),
                listOf(
                    Collector(Analytics.nameLog) to Analytics.levelOff,
                    Collector(Analytics.nameYandex) to Analytics.levelAll,
                    Collector(Analytics.nameCrashlytics) to Analytics.levelAll
                )
            )
        )

        private fun Analytics.Analytic.copy(
            name: String = this.name,
            level: Int = this.level
        ) = Analytics.Analytic(name, level)

        private class Collector(override val name: String) : Metrics.Collector {

            override fun onActivityResume(act: Activity) {
                throw AssertionError()
            }

            override fun onActivityPause(act: Activity) {
                throw AssertionError()
            }

            override fun sendEvent(name: String, args: MutableMap<String, Any>) {
                throw AssertionError()
            }

            override fun setLocation(loc: AndroidLocation?) {
                throw AssertionError()
            }
        }
    }

    @Test
    fun `should return correct analytics level`() {
        val repo = AnalyticsRepository(defaults, AdjustKit(mock()))

        repo.update(Analytics(analytics))
        val actual = expected.map { repo.getAnalyticsLevel(it.first) }
        assertEquals(expected.map { it.second }, actual)
    }
}
