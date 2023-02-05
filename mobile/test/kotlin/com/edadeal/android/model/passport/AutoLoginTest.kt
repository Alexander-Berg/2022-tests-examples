package com.edadeal.android.model.passport

import com.edadeal.android.helpers.StatefulMock
import com.edadeal.android.metrics.AutoLoginDescription
import com.edadeal.android.metrics.AutoLoginException
import com.edadeal.android.model.auth.passport.PassportApiFacade
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.yandex.passport.api.exception.PassportAutoLoginImpossibleException
import com.yandex.passport.api.exception.PassportAutoLoginRetryRequiredException
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.internal.verification.Times

class AutoLoginTest : MigrationDelegateTest() {

    private fun prepare(
        isAuthorized: Boolean,
        amAllowed: Boolean,
        autoLoginResult: () -> PassportApiFacade.AutoLoginResult
    ): StatefulMock<Boolean> {
        whenever(prefs.isAuthorized).then { isAuthorized }
        whenever(passportApi.autoLogin()).then { autoLoginResult() }
        return StatefulMock(amAllowed, { prefs::amAllowed.set(ArgumentMatchers.anyBoolean()) }, { prefs.amAllowed })
    }

    @Test
    fun `AutoLogin succeed when user is not authorized and amAllowed true`() {
        val amAllowed = prepare(false, true) {
            PassportApiFacade.AutoLoginResult(NOT_EMPTY_PASSPORT_UID, false)
        }

        val observer = delegate.startUp(meLoaded = true, deviceLoaded = true).test()
        observer.assertDelegateSuccess(amAllowed)
        verify(loginAction, Times(1)).invoke(any(), any())
    }

    @Test
    fun `AutoLogin succeed when user is not authorized and amAllowed false`() {
        val amAllowed = prepare(isAuthorized = false, amAllowed = false) {
            PassportApiFacade.AutoLoginResult(NOT_EMPTY_PASSPORT_UID, false)
        }

        val observer = delegate.startUp(true, true).test()
        observer.assertDelegateSuccess(amAllowed)
        verify(loginAction, Times(1)).invoke(any(), any())
    }

    @Test
    fun `AutoLogin failed when user is not authorized and AutoLogin throws exception, but am allowed gets true`() {
        val amAllowed = prepare(false, false) {
            throw AutoLoginException(PassportAutoLoginImpossibleException(PassportAutoLoginImpossibleException.CAUSE_CANT_GET_TOKEN))
        }

        val observer = delegate.startUp(meLoaded = true, deviceLoaded = true).test()
        observer.assertDelegateError(AutoLoginException(AutoLoginDescription.AmCantGetToken))
        verify(loginAction, Times(0)).invoke(any(), any())
        assert(amAllowed.value)
    }

    @Test
    fun `AutoLogin succeed when user is not authorized and AutoLogin throws PassportAutoLoginRetryRequiredException`() {
        val amAllowed = prepare(isAuthorized = false, amAllowed = false) {
            throw PassportAutoLoginRetryRequiredException(mock())
        }

        val observer = delegate.startUp(meLoaded = true, deviceLoaded = true).test()
        observer.assertDelegateSuccess(amAllowed)
        verify(loginAction, Times(1)).invoke(any(), any())
    }
}
