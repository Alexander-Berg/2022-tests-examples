package ru.yandex.disk.utils

import org.mockito.kotlin.*
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class IdCollisionResolutionTest {
    private val collisionHandler: (src: Int, pos: Int, clashPos: Int, id: Long) -> Unit = mock()

    private val sut = IdCollisionResolution(Int::toLong, collisionHandler)

    @Test
    fun shouldReturnSameOriginalIdWhenNoCollisionsOccurred() {
        assertThat(sut.getUniqueId(1, 1), equalTo(1L))
        assertThat(sut.getUniqueId(20, 2), equalTo(20L))
        assertThat(sut.getUniqueId(1, 1), equalTo(1L))
    }

    @Test
    fun shouldIncrementIdWhenCollisionsOccurred() {
        sut.getUniqueId(1, 1)
        sut.getUniqueId(2, 3)
        assertThat(sut.getUniqueId(1, 4), equalTo(3L))
        assertThat(sut.getUniqueId(1, 5), equalTo(4L))
    }

    @Test
    fun shouldNotSendAnalyticsWhenNoCollisionsOccurred() {
        (1..5).forEach {
            sut.getUniqueId(it, it)
        }

        verifyNoMoreInteractions(collisionHandler)
    }

    @Test
    fun shouldNotSendAnalyticsWhenCollisionsOccurred() {
        val src = 101

        sut.getUniqueId(src, 1)
        sut.getUniqueId(src, 2)
        sut.getUniqueId(src, 3)

        // position#2
        verify(collisionHandler).invoke(src, 2, 1, 101)

        // position#3
        verify(collisionHandler).invoke(src, 3, 1, 101)
        verify(collisionHandler).invoke(src, 3, 2, 102)

        verifyNoMoreInteractions(collisionHandler)
    }
}
