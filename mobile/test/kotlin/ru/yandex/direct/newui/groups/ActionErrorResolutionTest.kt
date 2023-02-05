// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.newui.groups

import android.content.res.Resources
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import ru.yandex.direct.newui.error.ErrorResolver
import ru.yandex.direct.newui.error.ErrorSeverity
import ru.yandex.direct.web.api5.result.ActionResult
import ru.yandex.direct.web.exception.ActionException

class ActionErrorResolutionTest {
    private val tag = "tag"

    private lateinit var resources: Resources

    private lateinit var resolver: ErrorResolver

    @Before
    fun runBeforeAnyTest() {
        resources = mock()
        resolver = mock()
    }

    @Test
    fun resolve_shouldHandleActionException() {
        val resolution = ActionErrorResolution(resources)
        resolution.attachResolver(resolver)

        val handled = resolution.resolve(tag, ActionException(ActionResult.Warning(0, "message", null)))

        assertThat(handled).isTrue()
        verify(resolver).acceptError(ErrorSeverity.MODAL_ERROR, "message")
    }

    @Test
    fun resolve_shouldIgnoreOtherExceptions() {
        val resolution = ActionErrorResolution(resources)
        resolution.attachResolver(resolver)

        val handled = resolution.resolve(tag, RuntimeException())

        assertThat(handled).isFalse()
        verify(resolver, never()).acceptError(any(), anyString())
    }
}
