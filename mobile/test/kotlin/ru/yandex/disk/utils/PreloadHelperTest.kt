package ru.yandex.disk.utils

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import kotlin.test.Test

class PreloadHelperTest {
    private var loadCount = 0
    private var consumeCount = 0
    private val loader = { loadCount += 1 }
    private val consumer = { _: Unit -> consumeCount += 1 }

    private val sut: PreloadHelper<Unit> = PreloadHelper(loader, consumer)

    @Test
    fun valueIsAutomaticallyLoadedOnProvideIfMissing() {
        sut.provide()
        sut.value = Unit
        sut.value = Unit

        assertThat(loadCount, equalTo(2))
        assertThat(consumeCount, equalTo(1))
        assertThat(sut.value, notNullValue())
    }

    @Test
    fun multipleProvidesAreCollapsedWhenCalledBeforeValueIsLoaded() {
        repeat(5) {
            sut.provide()
        }
        sut.value = Unit

        assertThat(consumeCount, equalTo(1))
    }
}
