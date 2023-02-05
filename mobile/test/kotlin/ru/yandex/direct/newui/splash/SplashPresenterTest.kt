// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.newui.splash

import android.content.Intent
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.anyOrNull
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.stub
import io.reactivex.Completable
import io.reactivex.Single
import org.junit.Test
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import ru.yandex.direct.domain.DirectAppPinCode
import ru.yandex.direct.eventbus.RxBus
import ru.yandex.direct.interactor.auth.PassportInteractor
import ru.yandex.direct.interactor.clients.CurrentClientInteractor
import ru.yandex.direct.interactor.dict.InterestsInteractor
import ru.yandex.direct.interactor.dict.LoadCurrencyInteractor
import ru.yandex.direct.interactor.dict.RegionsInteractor
import ru.yandex.direct.interactor.pincode.PinCodeInteractor
import ru.yandex.direct.interactor.push.PushInteractor
import ru.yandex.direct.newui.error.resolution.DefaultResolution
import ru.yandex.direct.remoteconfig.RemoteConfigInteractor
import ru.yandex.direct.ui.fragment.CommonDialogFragment
import ru.yandex.direct.utils.ViewEnvironment
import ru.yandex.direct.web.api5.IDirectApi5
import ru.yandex.direct.web.exception.ApiException

class SplashPresenterTest {
    @Test
    fun onError_shouldShowErrorDialog_ifGotAnyErrorExceptBadAuth() {
        MockedEnvironment().apply {
            initForErrorTest()

            presenter.attachView(view, null)
            presenter.onError(tag).accept(Throwable(errorMessage))
            scheduler.triggerActions()

            verify(view, times(1)).showErrorDialog(errorMessage, CommonDialogFragment.ActionOnClose.LOG_OUT)
            verify(view, never()).navigateToAccountManager(any())
            verify(authenticationInteractor, never()).performLogout()
        }
    }

    @Test
    fun onError_shouldRefreshToken_ifGotBadAuthError() {
        MockedEnvironment().apply {
            initForErrorTest()

            presenter.attachView(view, null)
            presenter.beginLoginSequence()
            presenter.onError(tag).accept(ApiException(IDirectApi5.ErrorCode.BAD_AUTH_TOKEN, null))
            scheduler.triggerActions()

            verify(view, never()).showErrorDialog(errorMessage, CommonDialogFragment.ActionOnClose.LOG_OUT)
            verify(authenticationInteractor, times(1)).performTokenRefresh()
            verify(view, times(1)).navigateToAccountManager(any())
        }
    }

    @Test
    fun onAgencyClientSelected_shouldPerformLogout_ifGotNull() {
        MockedEnvironment().apply {
            initForErrorTest()

            presenter.attachView(view, null)
            scheduler.triggerActions()
            presenter.onAgencyClientSelected(null)
            scheduler.triggerActions()

            verify(authenticationInteractor).performLogout()
        }
    }

    @Test
    fun pinCodeReset_shouldReturnToPinCodeScreen_ifErrorOccurredDuringPinCodeReset() {
        val pinCodeString = "1111"
        MockedEnvironment().apply {
            pinCodeInteractor.stub {
                on { hardResetPinCode() } doReturn Completable.error(Throwable())
                on { pinCode } doReturn Single.just(DirectAppPinCode.fromString(pinCodeString))
            }
            presenter.attachView(view, null)
            scheduler.triggerActions()
            verify(view).beginFadeInAnimation()
            presenter.beginLoginSequence()
            scheduler.triggerActions()
            verify(view).validatePinCode(pinCodeString)
            presenter.onPinCodeResult(mock { on { isForgotten } doReturn true })
            scheduler.triggerActions()
            verify(view).showErrorDialog(
                anyOrNull(),
                eq(CommonDialogFragment.ActionOnClose.DISMISS),
                eq(SplashPresenter.PIN_CODE_RESET_ERROR_DIALOG_ID)
            )
            presenter.onDialogConfirm(SplashPresenter.PIN_CODE_RESET_ERROR_DIALOG_ID)
            scheduler.triggerActions()
            verify(view, times(2)).validatePinCode(pinCodeString)
        }
    }

    private class MockedEnvironment : ViewEnvironment() {
        val tag = "tag"
        val errorMessage = "error message"

        val view = mock<SplashView>().stubViewMethods()

        val pinCodeInteractor = mock<PinCodeInteractor> {
            on { pinCode } doReturn Single.just(DirectAppPinCode.fromString(null))
            on { syncStashedPinCode() } doReturn Completable.complete()
        }
        val loadRegionsInteractor = mock<RegionsInteractor>()
        val loadCurrencyInteractor = mock<LoadCurrencyInteractor>()
        val currentClientInteractor = mock<CurrentClientInteractor>()
        val authenticationInteractor = mock<PassportInteractor> {
            on { performLogout() } doReturn Completable.complete()
            on { createAccountManagerIntent() } doReturn mock<Intent>()
        }

        val pushSettingsInteractor = mock<PushInteractor> {
            on { trySubscribeOnStartup() } doReturn Completable.complete()
        }

        val interestsInteractor = mock<InterestsInteractor> {
            on { loadInterestsOnStartup() } doReturn Completable.complete()
        }

        val remoteConfigInteractor = mock<RemoteConfigInteractor> {
            on { tryLoadRemoteConfig() } doReturn Completable.complete()
        }

        val resolution = DefaultResolution()

        val rxBus = RxBus()

        val presenter = SplashPresenter(mock(), resolution, currentClientInteractor, loadCurrencyInteractor,
                loadRegionsInteractor, authenticationInteractor, scheduler, mock(), mock(), pinCodeInteractor,
                pushSettingsInteractor, mock(), interestsInteractor, remoteConfigInteractor, rxBus)

        fun initForErrorTest() {
            currentClientInteractor.stub {
                on { setCurrentClientInfo(anyOrNull()) } doReturn Completable.complete()
            }
            authenticationInteractor.stub {
                on { performLogout() } doReturn Completable.complete()
                on { performTokenRefresh() } doReturn Single.just(false)
                on { isLoggedIn } doReturn listOf(true, false).map { bool -> Single.just(bool) }
            }
        }
    }
}