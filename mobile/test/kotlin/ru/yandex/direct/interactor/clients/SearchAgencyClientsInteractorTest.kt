// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.interactor.clients

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.schedulers.TestScheduler
import org.junit.Test
import ru.yandex.direct.domain.clients.ClientInfo
import ru.yandex.direct.repository.clients.AgencyClientsLocalRepository
import ru.yandex.direct.repository.clients.ClientsLocalQuery
import ru.yandex.direct.repository.clients.ClientsRemoteQuery
import ru.yandex.direct.repository.clients.ClientsRemoteRepository

class SearchAgencyClientsInteractorTest {
    @Test
    fun searchAgencyClients_shouldLoadFromRemote_ifForcedFlag() {
        Environment().apply {
            interactor.searchAgencyClients("", true).subscribe()
            scheduler.triggerActions()
            verify(remoteRepo).fetch(ClientsRemoteQuery.ofAllAgencyClients())
        }
    }

    @Test
    fun searchAgencyClients_shouldLoadFromLocal_ifNotForced() {
        Environment().apply {
            whenever(localRepo.containsActualData(any())).thenReturn(true)
            whenever(localRepo.select(any())).thenReturn(listOf(localClient))
            interactor.searchAgencyClients("", false).subscribe()
            scheduler.triggerActions()
            verify(remoteRepo, never()).fetch(any())
        }
    }

    @Test
    fun searchAgencyClients_shouldPassSearchQueryToLocalRepo() {
        Environment().apply {
            val query = "search query"
            interactor.searchAgencyClients(query, true).subscribe()
            scheduler.triggerActions()
            verify(localRepo).select(ClientsLocalQuery.searchFor(query))
        }
    }

    class Environment {
        val clientLogin = "mdtester"
        val remoteClient = ClientInfo()
        val localClient = ClientInfo()
        val scheduler = TestScheduler()

        val localRepo = mock<AgencyClientsLocalRepository>()
        val remoteRepo = mock<ClientsRemoteRepository>()

        val interactor = SearchAgencyClientsInteractor(localRepo, remoteRepo, scheduler, scheduler)

        init {
            remoteClient.login = "remote client login"
            localClient.login = "local client login"
        }
    }
}