package ru.yandex.disk.utils

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class ParentVisibilityTrackerTest {
    private val sut = ParentVisibilityTracker<Int, Int>()

    @Test
    fun shouldReturnTrueWhenVisibleChildFirst() {
        assertThat(sut.onChildVisible(1, 1), equalTo(true))
    }

    @Test
    fun shouldReturnFalseWhenVisibleChildIsNotFirst() {
        sut.onChildVisible(1, 1)
        assertThat(sut.onChildVisible(1, 2), equalTo(false))
        assertThat(sut.onChildVisible(1, 3), equalTo(false))
    }

    @Test
    fun shouldReturnTrueWhenHiddenChildIsLast() {
        sut.onChildVisible(1, 1)
        sut.onChildVisible(1, 2)
        sut.onChildHidden(1, 2)
        assertThat(sut.onChildHidden(1, 1), equalTo(true))
    }

    @Test
    fun shouldReturnFalseWhenHiddenChildIsNotLast() {
        sut.onChildVisible(1, 1)
        sut.onChildVisible(1, 2)
        assertThat(sut.onChildHidden(1, 1), equalTo(false))
    }

    @Test
    fun shouldCleanUp() {
        sut.onChildVisible(1, 1)
        sut.onChildVisible(1, 2)
        sut.onChildVisible(2, 1)
        sut.onChildVisible(3, 1)

        sut.onChildHidden(3, 1)
        assertThat(sut.size, equalTo(2))

        sut.onChildHidden(1, 2)
        sut.onChildHidden(2, 1)
        sut.onChildHidden(1, 1)
        assertThat(sut.size, equalTo(0))
    }
}
