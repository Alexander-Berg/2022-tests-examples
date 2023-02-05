// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.newui.clients

import com.google.gson.Gson
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.doThrow
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.stub
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.yandex.passport.api.PassportUid
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import ru.yandex.direct.data.ApiSampleData
import ru.yandex.direct.db.client.ClientDao
import ru.yandex.direct.domain.clients.ClientInfo
import ru.yandex.direct.interactor.clients.SearchAgencyClientsInteractor
import ru.yandex.direct.newui.base.ItemClickEvent
import ru.yandex.direct.repository.clients.AgencyClientsLocalRepository
import ru.yandex.direct.repository.clients.ClientsRemoteQuery
import ru.yandex.direct.repository.clients.ClientsRemoteRepository
import ru.yandex.direct.utils.FunctionalTestEnvironment
import ru.yandex.direct.web.api5.IDirectApi5
import ru.yandex.direct.web.exception.ApiException
import java.sql.SQLException

class AgencyClientsPresenterFunctionalTest {
    @Test
    fun presenter_shouldShowClientList_onStartup() {
        TestEnvironment().apply {
            presenter.attachView(view, null)
            scheduler.triggerActions()
            verify(view, never()).showError(anyString())
            verify(view).showLoadingIndicator(true)
            verify(view).showLoadingIndicator(false)
            verify(view, times(2)).showSwipeRefresh(false)
            verify(view).setSwipeRefreshEnabled(true)
            verify(view).showNothingFound(false)
            verify(adapter).clearAll()
            verify(adapter).addAll(testingClients)
        }
    }

    @Test
    fun presenter_shouldShowCachedClients_ifAny() {
        TestEnvironment().apply {
            clientDao.deleteThenInsert(testingClients)
            presenter.attachView(view, null)
            scheduler.triggerActions()
            verify(clientsRemoteRepo, never()).fetch(any())
            verify(adapter).addAll(testingClients)
        }
    }

    @Test
    fun presenter_shouldFetchClients_ifCacheIsEmpty() {
        TestEnvironment().apply {
            presenter.attachView(view, null)
            scheduler.triggerActions()
            val captor = ArgumentCaptor.forClass(ClientsRemoteQuery::class.java)
            verify(clientsRemoteRepo).fetch(captor.capture())
            assertThat(captor.value).isEqualToComparingFieldByField(ClientsRemoteQuery.ofAllAgencyClients())
        }
    }

    @Test
    fun presenter_shouldShowError_ifLocalQueryFailed() {
        TestEnvironment().apply {
            throwInLocalRepo = true
            presenter.attachView(view, null)
            scheduler.triggerActions()
            verify(view).showError(errorMessage)
        }
    }

    @Test
    fun presenter_shouldShowError_ifRemoteQueryFailed() {
        TestEnvironment().apply {
            whenever(clientsRemoteRepo.fetch(any())).doThrow(RuntimeException())
            presenter.attachView(view, null)
            scheduler.triggerActions()
            verify(view).showError(errorMessage)
        }
    }

    @Test
    fun presenter_shouldFinishView_ifGotBadAuthError() {
        TestEnvironment().apply {
            whenever(clientsRemoteRepo.fetch(any()))
                    .doThrow(ApiException(IDirectApi5.ErrorCode.BAD_AUTH_TOKEN, ""))
            presenter.attachView(view, null)
            scheduler.triggerActions()
            verify(view, never()).finishWithResult(any())
            verify(view).navigateToAccountManager()
        }
    }

    @Test
    fun presenter_shouldFinishWithResult_ifClientClicked() {
        TestEnvironment().apply {
            val clicks = PublishSubject.create<ItemClickEvent<ClientInfo>>()
            whenever(adapter.clicks).doReturn(clicks.subscribeOn(scheduler))
            presenter.attachView(view, null)
            scheduler.triggerActions()
            clicks.onNext(ItemClickEvent(mock(), testingClients[0]))
            scheduler.triggerActions()
            verify(view, never()).navigateToAccountManager()
            verify(view).finishWithResult(testingClients[0])
        }
    }

    @Test
    fun presenter_shouldFilterClients_ifSearchApplied() {
        TestEnvironment().apply {
            val searchQueries = PublishSubject.create<String>()
            whenever(view.searchQueryChanges).doReturn(searchQueries.subscribeOn(scheduler))

            presenter.attachView(view, null)
            scheduler.triggerActions()
            verify(adapter).clearAll()
            verify(adapter).addAll(testingClients)

            searchQueries.onNext("subuser-")
            scheduler.triggerActions()
            verify(adapter, times(2)).clearAll()
            verify(adapter, times(2)).addAll(testingClients)

            searchQueries.onNext("subuser-leha")
            scheduler.triggerActions()
            verify(adapter, times(3)).clearAll()
            verify(adapter).addAll(emptyList())
        }
    }

    @Test
    fun presenter_shouldShowNothingFound_ifNoData() {
        TestEnvironment().apply {
            val searchQueries = PublishSubject.create<String>()
            whenever(view.searchQueryChanges).doReturn(searchQueries.subscribeOn(scheduler))

            presenter.attachView(view, null)
            scheduler.triggerActions()
            searchQueries.onNext("android application with clean architecture")
            scheduler.triggerActions()

            verify(adapter).addAll(emptyList())
            verify(view).showNothingFound(true)
        }
    }

    @Test
    fun presenter_shouldUpdateData_onPullToRefresh() {
        TestEnvironment().apply {
            val swipes = PublishSubject.create<Any>()
            whenever(view.refreshSwipes).doReturn(swipes.subscribeOn(scheduler))

            presenter.attachView(view, null)
            scheduler.triggerActions()
            verify(clientsRemoteRepo, times(1)).fetch(any())
            verify(adapter, times(1)).addAll(testingClients)

            swipes.onNext(Any())
            scheduler.triggerActions()
            verify(clientsRemoteRepo, times(2)).fetch(any())
            verify(adapter, times(2)).addAll(testingClients)
        }
    }

    @Test
    fun presenter_shouldRespectSearchQuery_onPullToRefresh() {
        TestEnvironment().apply {
            val swipes = PublishSubject.create<Any>()
            whenever(view.refreshSwipes).doReturn(swipes.subscribeOn(scheduler))

            val searchQueries = PublishSubject.create<String>()
            whenever(view.searchQueryChanges).doReturn(searchQueries.subscribeOn(scheduler))

            presenter.attachView(view, null)
            scheduler.triggerActions()

            searchQueries.onNext("subuser-leha")
            scheduler.triggerActions()

            swipes.onNext(Any())
            scheduler.triggerActions()

            verify(view, times(2)).showNothingFound(true)
            verify(adapter, times(2)).addAll(emptyList())
        }
    }

    @Test
    fun presenter_shouldFinishView_ifBackClickPressed() {
        TestEnvironment().apply {
            val backClicks = PublishSubject.create<Any>()
            whenever(view.backClicks).doReturn(backClicks)

            presenter.attachView(view, null)
            scheduler.triggerActions()

            backClicks.onNext(Any())
            scheduler.triggerActions()

            verify(view).finish()
        }
    }

    private class TestEnvironment : FunctionalTestEnvironment() {
        val errorMessage = "error"
        val agencyLogin = "agencyLogin"

        val testingClients = listOf(ApiSampleData.subclientInfo)

        var throwInLocalRepo = false

        val clientDao = object : ClientDao(dbHelper, Gson()) {
            val clients = mutableListOf<ClientInfo>()

            override fun searchLike(query: String?, columns: MutableList<String>?, orderBy: String?,
                    limit: String?, where: String?, vararg args: String?): MutableList<ClientInfo> {
                if (throwInLocalRepo) {
                    throw SQLException()
                }
                return clients.filter { query == null || (it.login?.contains(query) ?: false) }.toMutableList()
            }

            override fun getAll(orderBy: String?, limit: String?, where: String?,
                    vararg args: String?): MutableList<ClientInfo> {
                if (throwInLocalRepo) {
                    throw SQLException()
                }
                return clients
            }

            override fun deleteThenInsert(values: MutableList<ClientInfo>) {
                clients.clear()
                clients.addAll(values)
            }

            override fun getCount() = clients.size.toLong()
        }

        val clientsRemoteRepo = mock<ClientsRemoteRepository> {
            on { fetch(any()) } doReturn testingClients
        }

        val clientsLocalRepo = AgencyClientsLocalRepository(clientDao)
        val interactor = SearchAgencyClientsInteractor(clientsLocalRepo, clientsRemoteRepo, scheduler, scheduler)

        val presenter = AgencyClientsPresenter(defaultErrorResolution, scheduler, interactor, passportInteractor)

        val adapter = mock<AgencyClientsAdapter> {
            on { clicks } doReturn Observable.never()
            on { contextClicks } doReturn Observable.never()
        }

        val view = mock<AgencyClientsView>().stubAdapterViewMethods(adapter).stub {
            on { backClicks } doReturn Observable.never()
            on { refreshSwipes } doReturn Observable.never()
            on { searchQueryChanges } doReturn Observable.never()
        }

        init {
            configuration.stub {
                on { passportUid } doReturn PassportUid.Factory.from(1)
            }

            resources.stub {
                on { getString(anyInt()) } doReturn errorMessage
            }
        }
    }
}