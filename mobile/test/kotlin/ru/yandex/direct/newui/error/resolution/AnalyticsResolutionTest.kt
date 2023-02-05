// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.newui.error.resolution

import com.nhaarman.mockito_kotlin.mock
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mockito.*
import ru.yandex.direct.DirectAppAnalytics

class AnalyticsResolutionTest {
    val tag = "tag"

    @Test
    fun resolve_always_shouldReturnFalse() {
        val analytics = mock<DirectAppAnalytics>()
        assertThat(AnalyticsResolution(analytics).resolve(tag, Throwable())).isFalse()
    }

    @Test
    fun resolve_always_shouldSendErrorToAnalytics() {
        val analytics = mock<DirectAppAnalytics>()
        val throwable = Throwable()
        assertThat(AnalyticsResolution(analytics).resolve(tag, throwable)).isFalse()
        verify(analytics, times(1)).sendError(tag, throwable)
    }
}
