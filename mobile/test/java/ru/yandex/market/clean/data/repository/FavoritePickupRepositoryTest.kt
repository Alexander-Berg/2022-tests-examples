package ru.yandex.market.clean.data.repository

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.checkout.data.repository.CheckoutGeneralInfoRepository
import ru.yandex.market.clean.data.fapi.source.pickup.PickUpFapiClient
import ru.yandex.market.clean.data.mapper.FavoritePickupMapper
import ru.yandex.market.clean.data.model.dto.pickup.FavoritePickupDto
import ru.yandex.market.clean.data.model.dto.pickup.favoritePickupDtoTestInstance
import ru.yandex.market.clean.data.store.FavoritePickupStreamDataStore
import ru.yandex.market.clean.domain.model.pickup.FavoritePickup
import ru.yandex.market.clean.domain.model.pickup.favoritePickupTestInstance
import ru.yandex.market.common.schedulers.NetworkingScheduler
import ru.yandex.market.domain.auth.model.authTokenTestInstance

class FavoritePickupRepositoryTest {

    private val subject = PublishSubject.create<List<FavoritePickup>>()

    private val networkScheduler = NetworkingScheduler(Schedulers.trampoline())
    private val favoritePickupMapper = mock<FavoritePickupMapper> {
        on { map(any<List<FavoritePickupDto>>()) } doReturn emptyList()
    }

    private val pickUpFapiClient = mock<PickUpFapiClient> {
        on { getFavoritePickups() } doReturn Single.just(emptyList())
    }

    private val favoritePickupStreamDataStore = mock<FavoritePickupStreamDataStore> {
        on { getStream(any(), anyOrNull()) } doReturn subject
    }
    private val checkoutGeneralInfoRepository = mock<CheckoutGeneralInfoRepository> {
        on { setUserFavoritePickups(any()) } doReturn Completable.complete()
    }

    private val favoritePickupRepository = FavoritePickupRepository(
        networkScheduler,
        favoritePickupMapper,
        favoritePickupStreamDataStore,
        checkoutGeneralInfoRepository,
        pickUpFapiClient
    )

    @Test
    fun `Check stream starts with value from network`() {
        val firstList = listOf(favoritePickupDtoTestInstance(id = "1"))
        val mappedFirstList = listOf(favoritePickupTestInstance(id = "1"))
        whenever(favoritePickupMapper.map(firstList)).thenReturn(mappedFirstList)
        whenever(pickUpFapiClient.getFavoritePickups()).thenReturn(Single.just(firstList))

        favoritePickupRepository.getFavoritePickupsStream(101, authTokenTestInstance())
            .test()
            .assertValue(mappedFirstList)
            .assertNotTerminated()
            .assertNoErrors()
    }

    @Test
    fun `Check stream doesn't terminate if fapi throws error and emit emptyList()`() {
        whenever(pickUpFapiClient.getFavoritePickups()).thenReturn(Single.error(RuntimeException()))
        favoritePickupRepository.getFavoritePickupsStream(101, authTokenTestInstance()).test()
            .assertNotTerminated()
            .assertNoErrors()
            .assertValue { it.isEmpty() }
    }
}
