// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.newui.error.resolution

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import org.junit.Test
import org.mockito.Mockito.*

class ChainedResolutionTest {
    @Test
    fun resolve_shouldBeCalledInOrder_lastAddedCalledFirst() {
        ResolutionChain(false, false, false).apply {
            resolve()
            inOrder.verify(first, times(1)).resolve(tag, throwable)
            inOrder.verify(second, times(1)).resolve(tag, throwable)
            inOrder.verify(third, times(1)).resolve(tag, throwable)
        }
    }

    @Test
    fun resolve_shouldBreakChain_ifErrorResolved() {
        ResolutionChain(false, true, false).apply {
            resolve()
            inOrder.verify(first, times(1)).resolve(tag, throwable)
            inOrder.verify(second, times(1)).resolve(tag, throwable)
            inOrder.verify(third, never()).resolve(tag, throwable)
        }
    }

    private class ResolutionChain(firstStatus: Boolean, secondStatus: Boolean, thirdStatus: Boolean) {
        val tag = "tag"
        val throwable = Throwable()

        val first = mock<ErrorResolution> {
            on { resolve(tag, throwable) } doReturn firstStatus
        }

        val second = mock<ErrorResolution> {
            on { resolve(tag, throwable) } doReturn secondStatus
        }

        val third = mock<ErrorResolution> {
            on { resolve(tag, throwable) } doReturn thirdStatus
        }

        val inOrder = inOrder(first, second, third)!!
        val resolution = ChainedResolution(third).after(second).after(first)

        fun resolve() = resolution.resolve(tag, throwable)
    }
}