// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.newui.clients

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.stub
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.junit.Test
import org.mockito.Mockito.anyBoolean
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.anyString
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import ru.yandex.direct.interactor.auth.PassportInteractor
import ru.yandex.direct.interactor.clients.SearchAgencyClientsInteractor
import ru.yandex.direct.newui.error.ErrorSeverity
import ru.yandex.direct.newui.error.resolution.DefaultResolution
import ru.yandex.direct.utils.ViewEnvironment
import ru.yandex.direct.web.api5.IDirectApi5
import ru.yandex.direct.web.exception.ApiException

class AgencyClientsPresenterTest {
    @Test
    fun onError_shouldShowErrorBox_onError() {
        MockedEnvironment().apply {
            presenter.attachView(view, null)
            presenter.onError(tag).accept(Throwable())
            scheduler.triggerActions()
            verify(view, times(2)).showError(defaultErrorMessage)
        }
    }

    @Test
    fun onError_shouldFinishActivity_onBadAuthError() {
        MockedEnvironment().apply {
            val errorMessage = "testing error"
            presenter.attachView(view, null)
            presenter.onError(tag).accept(ApiException(IDirectApi5.ErrorCode.BAD_AUTH_TOKEN, errorMessage))
            scheduler.triggerActions()
            verify(view, never()).showError(errorMessage)
            verify(view, times(1)).navigateToAccountManager()
            verify(authenticationInteractor, times(1)).performTokenRefresh()
        }
    }

    @Test
    fun onErrorResolved_shouldShowError_onWarning() {
        runErrorSeverityTest(ErrorSeverity.WARNING)
    }

    @Test
    fun onErrorResolved_shouldShowError_onError() {
        runErrorSeverityTest(ErrorSeverity.ERROR)
    }

    @Test
    fun onErrorResolved_shouldShowError_onModalError() {
        runErrorSeverityTest(ErrorSeverity.MODAL_ERROR)
    }

    private fun runErrorSeverityTest(severity: ErrorSeverity) {
        MockedEnvironment().apply {
            presenter.attachView(view, null)
            presenter.onErrorResolved(severity, defaultErrorMessage)
            verify(view).showError(defaultErrorMessage)
        }
    }

    private class MockedEnvironment : ViewEnvironment() {
        val tag = "tag"

        val defaultErrorMessage = "Default error message"

        val authenticationInteractor = mock<PassportInteractor> {
            on { performLogout() } doReturn Completable.complete()
            on { performTokenRefresh() } doReturn Single.just(false)
        }

        val searchClientsInteractor = mock<SearchAgencyClientsInteractor> {
            on { searchAgencyClients(anyString(), anyBoolean()) } doReturn Single.just(emptyList())
        }

        val adapter = mock<AgencyClientsAdapter> {
            on { clicks } doReturn Observable.never()
            on { contextClicks } doReturn Observable.never()
        }

        val view = mock<AgencyClientsView>().stubAdapterViewMethods(adapter).stub {
            on { backClicks } doReturn Observable.never()
            on { refreshSwipes } doReturn Observable.never()
            on { searchQueryChanges } doReturn Observable.never()
        }

        val resolution = DefaultResolution()
        val presenter = AgencyClientsPresenter(resolution, scheduler, searchClientsInteractor,
                authenticationInteractor)

        init {
            resources.stub { on { getString(anyInt()) } doReturn defaultErrorMessage }
        }
    }
}