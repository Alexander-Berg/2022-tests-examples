// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.newui.error.resolution

import android.content.res.Resources
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.eq
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import ru.yandex.direct.Configuration
import ru.yandex.direct.interactor.auth.PassportInteractor
import ru.yandex.direct.newui.error.ErrorResolver
import ru.yandex.direct.newui.error.ErrorSeverity
import ru.yandex.direct.web.api5.IDirectApi5
import ru.yandex.direct.web.exception.ApiException

class PaymentResolutionTest {
    @Test
    fun resolve_shouldNotDoAnySideEffects_ifGotInvalidPaymentTokenError() {
        TestEnvironment().apply {
            assertThat(resolution.resolve(tag, payTokenException)).isTrue()
            verify(configuration, never()).removeYandexMoneyToken()
            verify(resolver, times(1)).acceptError(ErrorSeverity.MODAL_ERROR, expectedErrorMessage)
        }
    }

    @Test
    fun resolve_shouldDoNothing_ifGotOtherError() {
        TestEnvironment().apply {
            assertThat(resolution.resolve(tag, authException)).isFalse()
            verify(configuration, never()).removeYandexMoneyToken()
            verify(resolver, never()).acceptError(any(), any<String>())
            verify(resolver, never()).acceptError(any(), anyInt())
        }
    }

    private class TestEnvironment {
        val tag = "tag"
        val userLogin = "tester"
        val expectedErrorMessage = "error, $userLogin"
        val payTokenException = ApiException(IDirectApi5.ErrorCode.INVALID_PAY_TOKEN, null)
        val authException = ApiException(IDirectApi5.ErrorCode.BAD_AUTH_TOKEN, null)

        val passportInteractor = mock<PassportInteractor> {
            on { accountName } doReturn userLogin
        }

        val resources = mock<Resources> {
            on { getString(anyInt(), eq(userLogin)) } doReturn expectedErrorMessage
        }

        val configuration = mock<Configuration>()
        val resolver = mock<ErrorResolver>()

        val resolution = PaymentResolution(passportInteractor, resources)

        init {
            resolution.attachResolver(resolver)
        }
    }
}