package ru.yandex.market.clean.data.repository.express

import dagger.Lazy
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.clean.data.fapi.source.express.ExpressFapiClient
import ru.yandex.market.clean.data.repository.hyperlocal.HyperlocalCoordinatesRepository
import ru.yandex.market.common.schedulers.DataSchedulers

class ExpressEntryPointRepositoryTest {

    private val expressIsEntryPointDataStore = mock<ExpressIsEntryPointDataStore>()
    private val hyperlocalCoordinatesRepository = mock<HyperlocalCoordinatesRepository>()
    private val dataSchedulers = mock<DataSchedulers> {
        on { worker } doReturn Schedulers.trampoline()
    }
    private val expressFapiClient = mock<ExpressFapiClient>()

    private val expressFapiClientLazy = mock<Lazy<ExpressFapiClient>> {
        on { get() } doReturn expressFapiClient
    }
    private val expressEntryPointRepository = ExpressEntryPointRepository(
        expressIsEntryPointDataStore,
        hyperlocalCoordinatesRepository,
        dataSchedulers,
        expressFapiClientLazy
    )

    @Test
    fun `test observeIsExpressEntryPoint()`() {
        whenever(expressIsEntryPointDataStore.observeIsExpressEntryPoint())
            .thenReturn(Observable.just(true))
        expressEntryPointRepository.observeIsExpressEntryPoint()
            .test()
            .assertNoErrors()
            .assertResult(true)
        verify(expressIsEntryPointDataStore).observeIsExpressEntryPoint()
    }

}