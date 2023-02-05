package ru.yandex.market.clean.data.fapi.source.plustrial

import com.google.gson.Gson
import io.reactivex.Completable
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.isA
import org.mockito.kotlin.mock
import ru.yandex.market.base.network.fapi.FapiEndpoint
import ru.yandex.market.clean.data.fapi.contract.plustrial.ActivateYandexPlusTrialContract
import ru.yandex.market.common.network.fapi.ReactiveFapiContractProcessor
import ru.yandex.market.common.network.fapi.FapiEndpoints

class ProductionPlusTrialFapiClientTest {

    private val blueFapi = mock<FapiEndpoint>()
    private val gson = mock<Gson>()
    private val fapiContractProcessor = mock<ReactiveFapiContractProcessor> {
        on {
            processCompletable(
                eq(blueFapi),
                isA<ActivateYandexPlusTrialContract>()
            )
        } doReturn Completable.complete()
    }
    private val fapiEndpoints = mock<FapiEndpoints> {
        on { blueFapi } doReturn blueFapi
    }

    private val client = ProductionPlusTrialFapiClient(gson, fapiContractProcessor, fapiEndpoints)

    @Test
    fun `Used ActivateYandexPlusTrialContract for get result`() {
        client.getPlusTrial().test().assertComplete()
    }
}