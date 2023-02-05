package ru.yandex.market.base.network.fapi.request.executor

import org.assertj.core.api.Assertions.assertThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.junit.Test
import ru.yandex.market.base.network.fapi.FapiVersion
import ru.yandex.market.base.network.fapi.contract.FapiContract

class FapiContractSplitterTest {

    private val splitter = FapiJsonContractSplitter()

    @Test
    fun `Returns no groups if there is no contracts to split`() {
        assertThat(splitter.split(emptyList())).isEmpty()
    }

    @Test
    fun `Contract with same api version are groups`() {
        val firstContract = mock<FapiContract<Any>> {
            on { apiVersion } doReturn FapiVersion { "v1" }
        }
        val secondContract = mock<FapiContract<Any>> {
            on { apiVersion } doReturn FapiVersion { "v1" }
        }
        assertThat(splitter.split(listOf(firstContract, secondContract))).contains(
            FapiJsonContractGroup("v1", listOf(firstContract, secondContract))
        )
    }

    @Test
    fun `Contracts splits by api version`() {
        val v1Contract = mock<FapiContract<Any>> {
            on { apiVersion } doReturn FapiVersion { "v1" }
        }
        val v2Contract = mock<FapiContract<Any>> {
            on { apiVersion } doReturn FapiVersion { "v2" }
        }
        assertThat(splitter.split(listOf(v1Contract, v2Contract))).contains(
            FapiJsonContractGroup("v1", listOf(v1Contract)),
            FapiJsonContractGroup("v2", listOf(v2Contract))
        )
    }

    @Test
    fun `Isolated contracts separates from unified`() {
        val unifiedContract = mock<FapiContract<Any>> {
            on { apiVersion } doReturn FapiVersion { "v1" }
        }
        val isolatedContract = mock<FapiContract<Any>> {
            on { apiVersion } doReturn FapiVersion { "v1" }
            on { isolated } doReturn true
        }
        assertThat(splitter.split(listOf(unifiedContract, isolatedContract))).contains(
            FapiJsonContractGroup("v1", listOf(unifiedContract)),
            FapiJsonContractGroup("v1", listOf(isolatedContract))
        )
    }

    @Test
    fun `Isolated contract separates from another isolated`() {
        val firstIsolatedContract = mock<FapiContract<Any>> {
            on { apiVersion } doReturn FapiVersion { "v1" }
            on { isolated } doReturn true
        }
        val secondIsolatedContract = mock<FapiContract<Any>> {
            on { apiVersion } doReturn FapiVersion { "v1" }
            on { isolated } doReturn true
        }
        assertThat(splitter.split(listOf(firstIsolatedContract, secondIsolatedContract))).contains(
            FapiJsonContractGroup("v1", listOf(firstIsolatedContract)),
            FapiJsonContractGroup("v1", listOf(secondIsolatedContract))
        )
    }

}