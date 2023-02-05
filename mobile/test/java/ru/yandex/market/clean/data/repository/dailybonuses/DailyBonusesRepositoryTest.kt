package ru.yandex.market.clean.data.repository.dailybonuses

import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.SingleSubject
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.yandex.market.base.network.common.Response
import ru.yandex.market.base.network.common.exception.CommunicationException
import ru.yandex.market.clean.data.fapi.contract.dailybonuses.ResolveDailyBonusesContract
import ru.yandex.market.clean.data.fapi.source.dailybonuses.DailyBonusesFapiClient
import ru.yandex.market.clean.data.mapper.dailybonuses.DailyBonusMapper
import ru.yandex.market.clean.data.model.dto.dailybonuses.DailyBonusDto
import ru.yandex.market.clean.data.model.dto.dailybonuses.dailyBonusDtoTestInstance
import ru.yandex.market.clean.data.store.dailybonuses.DailyBonusesDataStore
import ru.yandex.market.clean.domain.model.dailybonuses.PopupConfig
import ru.yandex.market.clean.domain.model.dailybonuses.dailyBonusInfoTestInstance
import ru.yandex.market.common.schedulers.NetworkingScheduler
import ru.yandex.market.safe.Safe

class DailyBonusesRepositoryTest {
    private val dailyBonusesSubject: SingleSubject<Pair<ResolveDailyBonusesContract.PopupConfigDto, List<DailyBonusDto>>> =
        SingleSubject.create()
    private val dailyBonusDto = dailyBonusDtoTestInstance()
    private val dailyBonusInfo = dailyBonusInfoTestInstance()
    private val dailyBonusPopupConfigDto = ResolveDailyBonusesContract.PopupConfigDto("1", "2")
    private val dailyBonusMapper = mock<DailyBonusMapper> {
        on { map(listOf(dailyBonusDto)) } doReturn listOf(Safe.value(dailyBonusInfo))
    }
    private val networkingScheduler = mock<NetworkingScheduler> {
        on { scheduler } doReturn Schedulers.trampoline()
    }
    private val dailyBonusesFapiClient = mock<DailyBonusesFapiClient> {
        on { getDailyBonusesAndPopupConfig() } doReturn dailyBonusesSubject
    }
    private val dailyBonusesDataStore = mock<DailyBonusesDataStore>()

    private val repository = DailyBonusesRepository(
        dailyBonusesFapiClient,
        dailyBonusMapper,
        networkingScheduler,
        dailyBonusesDataStore,
    )

    @Test
    fun `Return result from fapi client`() {
        dailyBonusesSubject.onSuccess(dailyBonusPopupConfigDto to listOf(dailyBonusDto))

        repository.getDailyBonusesAndPopupConfig()
            .test()
            .assertNoErrors()
            .assertValue(PopupConfig("1", "2") to listOf(Safe.value(dailyBonusInfo)))
    }

    @Test
    fun `Return error from fapi client`() {
        val error = CommunicationException(Response.BAD_REQUEST)
        dailyBonusesSubject.onError(error)

        repository.getDailyBonusesAndPopupConfig()
            .test()
            .assertError(error)
    }

    @Test
    fun `Subscribe get bonuses on network scheduler`() {
        repository.getDailyBonusesAndPopupConfig()
        verify(networkingScheduler).scheduler
    }
}