// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.newui.error.resolution

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.direct.R
import ru.yandex.direct.newui.error.ErrorResolver
import ru.yandex.direct.newui.error.ErrorSeverity

class DefaultResolutionTest {
    @Test
    fun resolve_shouldResolveCause_ifThrowableHasCause() {
        TestEnvironment().apply {
            assertThat(resolution.resolve(tag, wrapper)).isTrue()
            verify(resolver, times(1)).acceptError(ErrorSeverity.ERROR, causeMessage)
            verify(resolver, never()).acceptError(ErrorSeverity.ERROR, wrapperMessage)
        }
    }

    @Test
    fun resolve_shouldResolveThrowable_ifThrowableHasNoCause() {
        TestEnvironment().apply {
            assertThat(resolution.resolve(tag, throwableWithMessage)).isTrue()
            verify(resolver, times(1)).acceptError(ErrorSeverity.ERROR, throwableMessage)
        }
    }

    @Test
    fun resolve_shouldReturnDefaultMessage_ifThrowableHasNoMessage() {
        TestEnvironment().apply {
            assertThat(resolution.resolve(tag, throwableWithoutMessage)).isTrue()
            verify(resolver, times(1)).acceptError(ErrorSeverity.ERROR, R.string.unexpected_error)
        }
    }

    private class TestEnvironment {
        val tag = "tag"
        val causeMessage = "Cause"
        val wrapperMessage = "Wrapper"
        val throwableMessage = "Throwable"

        val cause = Throwable(causeMessage)
        val wrapper = Throwable(wrapperMessage, cause)
        val throwableWithMessage = Throwable(throwableMessage)
        val throwableWithoutMessage = Throwable()

        val resolution = DefaultResolution()
        val resolver = mock<ErrorResolver>()

        init {
            resolution.attachResolver(resolver)
        }
    }
}