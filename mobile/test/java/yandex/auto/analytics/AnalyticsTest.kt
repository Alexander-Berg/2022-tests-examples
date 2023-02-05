package ru.yandex.yandexnavi.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class AnalyticsTest {

    @Before
    fun setup() {
        Analytics.clear()
    }

    @Test
    fun `when reporting event then all reporters receive it`() {
        val events = listOf(
            Pair("event_with_null_params", null),
            Pair("event_with_param_with_null_value", mapOf(Pair("param1", null))),
            Pair("event_with_a_few_params", mapOf(Pair("param1", null), Pair("param1", Any())))
        )
        var counter = 0

        val firstReporter = TestReporter({ event, params ->
            checkEvents(events[counter], Pair(event, params))
        }, { _, _ -> fail() })

        val loggerReporter = TestReporter({ event, params ->
            checkEvents(events[counter], Pair(event, params))
        }, { _, _ -> fail() })

        Analytics.initializer()
            .add(firstReporter)
            .add(loggerReporter)
            .initialize()

        events.forEach { event ->
            Analytics.report(event.first, event.second)
            counter++
        }

        assertEquals(3, firstReporter.eventCounter)
        assertEquals(0, firstReporter.errorCounter)
        assertEquals(3, loggerReporter.eventCounter)
        assertEquals(0, loggerReporter.errorCounter)
    }

    @Test
    fun `when reporting error then all reporters receive it`() {
        val errors = listOf(
            Pair(NullPointerException(), "Some message for null pointer exception"),
            Pair(IllegalStateException(), null),
            Pair(Throwable(), "Common exception")
        )
        var counter = 0

        // messages are not reported so far
        val reporter1 = TestReporter(
            { _, _ -> fail() },
            { error, _ -> checkErrors(errors[counter].first, error) }
        )

        val reporter2 = TestReporter(
            { _, _ -> fail() },
            { error, _ -> checkErrors(errors[counter].first, error) }
        )

        Analytics.initializer()
            .add(reporter1)
            .add(reporter2)
            .initialize()

        errors.forEach { error ->
            Analytics.error(error.first)
            counter++
        }

        assertEquals(0, reporter1.eventCounter)
        assertEquals(3, reporter1.errorCounter)
        assertEquals(0, reporter2.eventCounter)
        assertEquals(3, reporter2.errorCounter)
    }

    private fun checkEvents(
        expectedEvent: Pair<String, Map<String, Any?>?>,
        actualEvent: Pair<String, Map<String, Any?>?>
    ) = assertEquals(expectedEvent.toString(), actualEvent.toString())

    private fun checkErrors(
        expectedError: Throwable,
        actualError: Throwable
    ) = assertEquals(expectedError, actualError)
}

private fun Analytics.clear() {
    this.REPORTERS.removeAll { true }
}
