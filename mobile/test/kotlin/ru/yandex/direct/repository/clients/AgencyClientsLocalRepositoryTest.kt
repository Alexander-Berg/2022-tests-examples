// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.repository.clients

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.anyOrNull
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import ru.yandex.direct.data.ApiSampleData
import ru.yandex.direct.db.WhereClause
import ru.yandex.direct.domain.clients.ClientInfo

class AgencyClientsLocalRepositoryTest {
    @Test
    fun select_shouldReturnEmptyList_ifHasNoDataAndGotEmptyQuery() {
        assertThat(repoWith(emptyList())
                .select(ClientsLocalQuery.searchFor("")))
                .isEmpty()
    }

    @Test
    fun select_shouldReturnEmptyList_ifHasNoDataAndGotSomeQuery() {
        assertThat(repoWith(emptyList())
                .select(ClientsLocalQuery.searchFor("inappropriate")))
                .isEmpty()
    }

    @Test
    fun select_shouldReturnData_ifHasDataAndGotAppropriateQuery() {
        assertThat(repoWith(listOf(ApiSampleData.clientInfo))
                .select(ClientsLocalQuery.searchFor("")))
                .containsExactly(ApiSampleData.clientInfo)
    }

    private fun repoWith(data: List<ClientInfo>) = AgencyClientsLocalRepository(mock {
        on { searchLike(anyString(), anyString(), anyOrNull(), anyOrNull<WhereClause>()) } doReturn data
        on { getAll(any(), anyOrNull(), anyOrNull()) } doReturn data
    })
}