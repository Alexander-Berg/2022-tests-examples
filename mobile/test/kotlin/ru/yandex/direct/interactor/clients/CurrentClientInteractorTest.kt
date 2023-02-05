// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.interactor.clients

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.doThrow
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.TestScheduler
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.direct.Configuration
import ru.yandex.direct.data.ApiSampleData
import ru.yandex.direct.domain.clients.ClientInfo
import ru.yandex.direct.repository.clients.ClientsRemoteRepository
import ru.yandex.direct.web.api5.IDirectApi5
import ru.yandex.direct.web.exception.ApiException

class CurrentClientInteractorTest {
    @Test
    fun getUserClientInfo_shouldEmit_ifConfigurationHasCurrentClient() {
        Environment().apply {
            whenever(configuration.currentClient).doReturn(ApiSampleData.clientInfo)
            val observer = TestObserver.create<ClientInfo>()
            interactor.currentClientInfo.subscribe(observer)
            scheduler.triggerActions()
            assertThat(observer.values()).usingRecursiveFieldByFieldElementComparator()
                    .containsExactly(ApiSampleData.clientInfo)
        }
    }

    @Test
    fun getUserClientInfo_shouldCompleteWithoutResult_ifConfigurationDoesNotHaveCurrentClient() {
        Environment().apply {
            whenever(configuration.currentClient).doReturn(null as ClientInfo?)
            val observer = TestObserver.create<ClientInfo>()
            interactor.currentClientInfo.subscribe(observer)
            scheduler.triggerActions()
            observer.assertNoValues()
        }
    }

    @Test
    fun setUserClientInfo_shouldUpdateClientInfoAndLastLoginTime() {
        Environment().apply {
            val observer = TestObserver.create<ClientInfo>()
            interactor.setCurrentClientInfo(ApiSampleData.clientInfo).subscribe(observer)
            scheduler.triggerActions()
            observer.assertComplete()
            verify(configuration).currentClient = ApiSampleData.clientInfo
            verify(configuration).lastLoginTime = any()
        }
    }

    @Test
    fun fetchUserClientInfo_shouldWork_onCorrectData() {
        Environment().apply {
            whenever(remoteRepo.fetch(any())).doReturn(listOf(ApiSampleData.clientInfo))
            val observer = TestObserver.create<ClientInfo>()
            interactor.fetchUserClientInfo().subscribe(observer)
            scheduler.triggerActions()
            verify(configuration).currentClient = ApiSampleData.clientInfo
            observer.assertNoErrors()
        }
    }

    @Test
    fun fetchUserClientInfo_shouldWork_withMultipleClients() {
        Environment().apply {
            val redundantClient = ClientInfo()
            redundantClient.login = "I'm not indent to be here!"
            whenever(remoteRepo.fetch(any())).doReturn(listOf(ApiSampleData.clientInfo, redundantClient))
            val observer = TestObserver.create<ClientInfo>()
            interactor.fetchUserClientInfo().subscribe(observer)
            scheduler.triggerActions()
            verify(configuration).currentClient = ApiSampleData.clientInfo
            verify(configuration, never()).currentClient = redundantClient
            observer.assertNoErrors()
        }
    }

    @Test
    fun fetchUserClientInfo_shouldThrow_ifGotEmptyList() {
        Environment().apply {
            whenever(remoteRepo.fetch(any())).doReturn(emptyList<ClientInfo>())
            val observer = TestObserver.create<ClientInfo>()
            interactor.fetchUserClientInfo().subscribe(observer)
            scheduler.triggerActions()
            observer.assertError(NoSuchElementException::class.java)
        }
    }

    @Test
    fun isAgency_true_ifFetchClientsWithoutErrors() {
        Environment().apply {
            whenever(remoteRepo.fetch(any())).doReturn(listOf(ApiSampleData.clientInfo))
            val observer = TestObserver.create<Boolean>()
            interactor.performRemoteIsAgencyCheck().subscribe(observer)
            scheduler.triggerActions()
            observer.assertValues(true)
        }
    }

    @Test
    fun isAgency_false_ifGotAccessError() {
        Environment().apply {
            whenever(remoteRepo.fetch(any()))
                    .doThrow(ApiException(IDirectApi5.ErrorCode.INSUFFICIENT_PRIVILEGES, null))
            val observer = TestObserver.create<Boolean>()
            interactor.performRemoteIsAgencyCheck().subscribe(observer)
            scheduler.triggerActions()
            observer.assertValues(false)
        }
    }

    @Test
    fun isAgency_shouldThrow_ifGotAnyOtherError() {
        Environment().apply {
            val error = ApiException(IDirectApi5.ErrorCode.INVALID_PAY_TOKEN, null)
            whenever(remoteRepo.fetch(any())).doThrow(error)
            val observer = TestObserver.create<Boolean>()
            interactor.performRemoteIsAgencyCheck().subscribe(observer)
            scheduler.triggerActions()
            observer.assertNoValues()
            observer.assertError(error)
        }
    }

    @Test
    fun isAgency_shouldReturnTrue_ifGotEmptyList() {
        Environment().apply {
            whenever(remoteRepo.fetch(any())).doReturn(emptyList<ClientInfo>())
            val observer = TestObserver.create<Boolean>()
            interactor.performRemoteIsAgencyCheck().subscribe(observer)
            scheduler.triggerActions()
            observer.assertValue(true)
            observer.assertComplete()
            observer.assertNoErrors()
        }
    }

    private class Environment {
        val configuration = mock<Configuration>()
        val remoteRepo = mock<ClientsRemoteRepository>()
        val scheduler = TestScheduler()

        val interactor = CurrentClientInteractor(remoteRepo, configuration, scheduler, scheduler)
    }
}