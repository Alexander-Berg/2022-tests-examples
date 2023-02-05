package com.yandex.frankenstein.agent.client

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import kotlin.concurrent.thread

private const val KEY = "key"
private const val VALUE = 42
private const val ANOTHER_VALUE = 100
private const val MILLIS_BEFORE_SET = 100L

class TestObjectStorageTest {

    private val testObjectStorage = TestObjectStorage()

    @Test
    fun testGetAfterSet() {
        testObjectStorage[KEY] = VALUE
        val actualValue: Int = testObjectStorage[KEY]

        assertThat(actualValue).isEqualTo(VALUE)
    }

    @Test
    fun testGetOrPutAfterSet() {
        testObjectStorage[KEY] = VALUE
        val actualValue = testObjectStorage.getOrPut(KEY) { ANOTHER_VALUE }

        assertThat(actualValue).isEqualTo(VALUE)
    }

    @Test
    fun testGetOrPutWithoutSet() {
        val actualValue = testObjectStorage.getOrPut(KEY) { VALUE }

        assertThat(actualValue).isEqualTo(VALUE)
    }

    @Test
    fun testGetOrWaitAfterSet() {
        testObjectStorage[KEY] = VALUE
        val actualValue: Int = testObjectStorage.getOrWait(KEY)

        assertThat(actualValue).isEqualTo(VALUE)
    }

    @Test
    fun testGetOrWaitBeforeSet() {
        thread {
            Thread.sleep(MILLIS_BEFORE_SET)
            testObjectStorage[KEY] = VALUE
        }
        val actualValue: Int = testObjectStorage.getOrWait(KEY)

        assertThat(actualValue).isEqualTo(VALUE)
    }
}
