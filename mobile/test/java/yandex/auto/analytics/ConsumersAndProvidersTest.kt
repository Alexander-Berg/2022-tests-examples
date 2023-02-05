package ru.yandex.yandexnavi.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.yandex.yandexnavi.logger.Logger

class ConsumersAndProvidersTest {

    @Test
    fun `when async analytics params initialized then all consumers receive them`() {
        val allParams = listOf("headid", "otherparams", "evenmore", "third", "4")
        val expectedParams1 = mapOf(allParams[0] to "111111111111", allParams[1] to "value")
        val expectedParams2 = mapOf(allParams[2] to "params")
        val expectedParams3 = mapOf(allParams[3] to "provider")
        val expectedParams4 = mapOf(allParams[4] to "null")

        val provider1 = TestParamsProvider(expectedParams1, expectedParams2)
        val provider2 = TestParamsProvider(expectedParams3, expectedParams4)

        val actualForConsumerAllParams = mutableMapOf<String, String>()
        val consumerAllParams = TestParamsConsumer(allParams.toSet()) {
            it.forEach { (key, value) -> actualForConsumerAllParams[key] = value }
        }
        val actualForConsumerSomeOfParams = mutableMapOf<String, String>()
        val consumerSomeOfParams = TestParamsConsumer(setOf(allParams[0], allParams[4])) {
            it.forEach { (key, value) -> actualForConsumerSomeOfParams[key] = value }
        }

        Analytics.initializer()
            .add(provider1)
            .add(provider2)
            .add(consumerAllParams)
            .add(consumerSomeOfParams)
            .initialize()

        assertEquals(0, consumerAllParams.counter)
        assertEquals(0, consumerSomeOfParams.counter)
        checkDoesNotContainParams(expectedParams1, actualForConsumerAllParams)
        checkDoesNotContainParams(expectedParams2, actualForConsumerAllParams)
        checkDoesNotContainParams(expectedParams3, actualForConsumerAllParams)
        checkDoesNotContainParams(expectedParams4, actualForConsumerAllParams)
        checkDoesNotContainParams(expectedParams1, actualForConsumerSomeOfParams)
        checkDoesNotContainParams(expectedParams2, actualForConsumerSomeOfParams)
        checkDoesNotContainParams(expectedParams3, actualForConsumerSomeOfParams)
        checkDoesNotContainParams(expectedParams4, actualForConsumerSomeOfParams)

        provider1.triggerFirstParams()

        assertEquals(1, consumerAllParams.counter)
        assertEquals(1, consumerSomeOfParams.counter)
        checkContainsParams(expectedParams1, actualForConsumerAllParams)
        checkDoesNotContainParams(expectedParams2, actualForConsumerAllParams)
        checkDoesNotContainParams(expectedParams3, actualForConsumerAllParams)
        checkDoesNotContainParams(expectedParams4, actualForConsumerAllParams)
        checkContainsParams(expectedParams1.filter { it.key == allParams[0] }, actualForConsumerSomeOfParams)
        checkDoesNotContainParams(expectedParams2, actualForConsumerSomeOfParams)
        checkDoesNotContainParams(expectedParams3, actualForConsumerSomeOfParams)
        checkDoesNotContainParams(expectedParams4, actualForConsumerSomeOfParams)

        provider2.triggerFirstParams()

        assertEquals(2, consumerAllParams.counter)
        assertEquals(2, consumerSomeOfParams.counter)
        checkContainsParams(expectedParams1, actualForConsumerAllParams)
        checkContainsParams(expectedParams3, actualForConsumerAllParams)
        checkDoesNotContainParams(expectedParams2, actualForConsumerAllParams)
        checkDoesNotContainParams(expectedParams4, actualForConsumerAllParams)
        checkContainsParams(expectedParams1.filter { it.key == allParams[0] }, actualForConsumerSomeOfParams)
        checkDoesNotContainParams(expectedParams2, actualForConsumerSomeOfParams)
        checkDoesNotContainParams(expectedParams3, actualForConsumerSomeOfParams)
        checkDoesNotContainParams(expectedParams4, actualForConsumerSomeOfParams)

        provider1.triggerSecondParams()

        assertEquals(3, consumerAllParams.counter)
        assertEquals(3, consumerSomeOfParams.counter)
        checkContainsParams(expectedParams1, actualForConsumerAllParams)
        checkContainsParams(expectedParams2, actualForConsumerAllParams)
        checkContainsParams(expectedParams3, actualForConsumerAllParams)
        checkDoesNotContainParams(expectedParams4, actualForConsumerAllParams)
        checkContainsParams(expectedParams1.filter { it.key == allParams[0] }, actualForConsumerSomeOfParams)
        checkDoesNotContainParams(expectedParams2, actualForConsumerSomeOfParams)
        checkDoesNotContainParams(expectedParams3, actualForConsumerSomeOfParams)
        checkDoesNotContainParams(expectedParams4, actualForConsumerSomeOfParams)

        provider2.triggerSecondParams()

        assertEquals(4, consumerAllParams.counter)
        assertEquals(4, consumerSomeOfParams.counter)
        checkContainsParams(expectedParams1, actualForConsumerAllParams)
        checkContainsParams(expectedParams2, actualForConsumerAllParams)
        checkContainsParams(expectedParams3, actualForConsumerAllParams)
        checkContainsParams(expectedParams4, actualForConsumerAllParams)
        checkContainsParams(expectedParams1.filter { it.key == allParams[0] }, actualForConsumerSomeOfParams)
        checkContainsParams(expectedParams4.filter { it.key == allParams[4] }, actualForConsumerSomeOfParams)
        checkDoesNotContainParams(expectedParams2, actualForConsumerSomeOfParams)
        checkDoesNotContainParams(expectedParams3, actualForConsumerSomeOfParams)
    }

    @Test
    fun `when not all consumed params are provided then log warning`() {
        val providedParam = "headid"
        val expectedParams = mapOf(providedParam to "111111111111")
        val provider = TestParamsProvider(expectedParams)

        val actualForConsumer = mutableMapOf<String, String>()
        val missingParams = setOf("missingparam1", "missingparam2")
        val consumer = TestParamsConsumer(mutableSetOf(providedParam).apply { addAll(missingParams) }) {
            it.forEach { (key, value) -> actualForConsumer[key] = value }
        }

        Analytics.initializer()
            .add(provider)
            .add(consumer)
            .initialize()

        println("Last logged: " + Logger.lastLogged)
        assertTrue(Logger.lastLogged?.contains(missingParams.toString()) ?: false)
        assertEquals(0, consumer.counter)
        checkDoesNotContainParams(expectedParams, actualForConsumer)

        provider.triggerFirstParams()
        assertEquals(1, consumer.counter)
        checkContainsParams(expectedParams, actualForConsumer)
    }

    private fun checkContainsParams(expected: Map<String, String>, actual: Map<String, String>) {
        expected.forEach {
            val value = actual[it.key]
            assertEquals(it.value, value)
        }
    }

    private fun checkDoesNotContainParams(expected: Map<String, String>, actual: Map<String, String>) {
        expected.forEach { assertNull(actual[it.key]) }
    }
}
