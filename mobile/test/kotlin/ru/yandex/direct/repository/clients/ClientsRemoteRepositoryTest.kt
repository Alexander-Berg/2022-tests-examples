// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.repository.clients

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.spy
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.Test
import retrofit2.Call
import retrofit2.Response
import ru.yandex.direct.data.ApiSampleData
import ru.yandex.direct.utils.CurrencyInitializer
import ru.yandex.direct.utils.MockedDirectApi5
import ru.yandex.direct.web.ApiInstanceHolder
import ru.yandex.direct.web.api5.clients.AgencyClientsResult
import ru.yandex.direct.web.api5.clients.ClientResult
import ru.yandex.direct.web.api5.request.AgencyClientsGetParams
import ru.yandex.direct.web.api5.request.BaseGet
import ru.yandex.direct.web.api5.request.ClientGetParams
import ru.yandex.direct.web.api5.result.BaseResult
import ru.yandex.direct.web.exception.ApiException
import ru.yandex.direct.web.exception.HttpResponseCodeException
import java.io.IOException

class ClientsRemoteRepositoryTest {
    @Test
    fun fetch_ofAllAgencyClients_shouldInvokeCorrectApiMethod() {
        TestEnvironment().apply {
            repo.fetch(ClientsRemoteQuery.ofAllAgencyClients())
            val paramsCaptor = argumentCaptor<BaseGet<AgencyClientsGetParams>>()
            verify(directApi, never()).getClient(any())
            verify(directApi, times(1)).getAgencyClients(paramsCaptor.capture())
            assertThat(paramsCaptor.firstValue.params)
                    .isEqualToComparingFieldByFieldRecursively(AgencyClientsGetParams.forAllClients())
        }
    }

    @Test
    fun fetch_ofSingleAgencyClient_shouldInvokeCorrectApiMethod() {
        TestEnvironment().apply {
            repo.fetch(ClientsRemoteQuery.ofAgencyClient(clientLogin))
            val paramsCaptor = argumentCaptor<BaseGet<AgencyClientsGetParams>>()
            verify(directApi, never()).getClient(any())
            verify(directApi, times(1)).getAgencyClients(paramsCaptor.capture())
            assertThat(paramsCaptor.firstValue.params)
                    .isEqualToComparingFieldByFieldRecursively(AgencyClientsGetParams.forSingleClient(clientLogin))
        }
    }

    @Test
    fun fetch_ofIndependentClient_shouldInvokeCorrectApiMethod() {
        TestEnvironment().apply {
            repo.fetch(ClientsRemoteQuery.ofIndependentClient())
            val paramsCaptor = argumentCaptor<BaseGet<ClientGetParams>>()
            verify(directApi, never()).getAgencyClients(any())
            verify(directApi, times(1)).getClient(paramsCaptor.capture())
            assertThat(paramsCaptor.firstValue.params)
                    .isEqualToComparingFieldByFieldRecursively(ClientGetParams())
        }
    }

    @Test
    fun convertToClientInfo_shouldWorkCorrectly_withNonNullArguments() {
        TestEnvironment().apply {
            CurrencyInitializer.injectTestDataInStaticFields()
            val clientGetItem = ApiSampleData.clientGetItem
            val clientInfos = repo.convertToClientInfo(listOf(clientGetItem))
            assertThat(clientInfos).hasSize(1)
            assertThat(clientInfos[0].login).isEqualTo(clientGetItem.login)
        }
    }

    @Test
    fun validate_shouldThrow_ifResponseIsNotSuccessful() {
        TestEnvironment().apply {
            assertThatExceptionOfType(HttpResponseCodeException::class.java)
                    .isThrownBy {
                        repo.validate<ClientResult>(
                                Response.error(404, ResponseBody.create(MediaType.parse("text/plain"), ""))
                        )
                    }
        }
    }

    @Test
    fun validate_shouldThrow_ifBaseResultContainsError() {
        TestEnvironment().apply {
            assertThatExceptionOfType(ApiException::class.java)
                    .isThrownBy { repo.validate(Response.success(ApiSampleData.errorFor<ClientResult>())) }
        }
    }

    @Test
    fun validate_shouldThrow_ifResponseBodyIsNull() {
        TestEnvironment().apply {
            assertThatExceptionOfType(IOException::class.java)
                    .isThrownBy { repo.validate(Response.success<BaseResult<ClientResult>>(null)) }
        }
    }

    @Test
    fun validate_shouldReturnClients_ifResponseSuccessAndContainsBody() {
        TestEnvironment().apply {
            val clients = repo.validate(Response.success<BaseResult<ClientResult>>(ApiSampleData.clientsResponse))
            assertThat(clients).isNotEmpty
            assertThat(clients[0].login).isEqualTo(ApiSampleData.clientGetItem.login)
        }
    }

    class TestEnvironment {
        val directApi = spy(ClientsDirectApi())
        val repo = ClientsRemoteRepository(ApiInstanceHolder.just(directApi))
    }

    open class ClientsDirectApi : MockedDirectApi5() {
        override fun getAgencyClients(body: BaseGet<AgencyClientsGetParams>): Call<BaseResult<AgencyClientsResult>> {
            return delegate.returningResponse(BaseResult(AgencyClientsResult(emptyList()))).getAgencyClients(body)
        }

        override fun getClient(body: BaseGet<ClientGetParams>): Call<BaseResult<ClientResult>> {
            return delegate.returningResponse(BaseResult(ClientResult(emptyList()))).getClient(body)
        }
    }

    companion object {
        const val clientLogin = "clientLogin"
    }
}