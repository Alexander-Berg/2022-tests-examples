package ru.yandex.market.clean.data.repository

import com.annimon.stream.Optional
import com.annimon.stream.OptionalLong
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.clean.data.executor.BindCoinsJobExecutor
import ru.yandex.market.clean.data.fapi.FrontApiDataSource
import ru.yandex.market.clean.data.fapi.dto.mergedUserBonusFapiDtoTestInstance
import ru.yandex.market.clean.data.fapi.source.usercoins.UserCoinsFapiClient
import ru.yandex.market.clean.data.mapper.SmartCoinCountMapper
import ru.yandex.market.clean.data.mapper.SmartCoinsCollectionMapper
import ru.yandex.market.clean.data.repository.smartshopping.SmartShoppingPreferencesRepository
import ru.yandex.market.clean.data.repository.smartshopping.SmartShoppingRepository
import ru.yandex.market.clean.data.store.SmartShoppingLocalDataStore
import ru.yandex.market.clean.domain.model.SmartCoinsCollection
import ru.yandex.market.common.schedulers.NetworkingScheduler
import ru.yandex.market.data.regions.SelectedRegionRepository
import ru.yandex.market.domain.auth.model.UserAccount
import ru.yandex.market.domain.auth.usecase.GetAuthTokenUseCase
import ru.yandex.market.domain.auth.usecase.GetUuidUseCase
import ru.yandex.market.internal.ApplicationEvent
import ru.yandex.market.internal.CheckoutTaskClosedEvent
import ru.yandex.market.internal.EventBus
import ru.yandex.market.internal.sync.Synchronized
import ru.yandex.market.internal.sync.Synchronizing
import ru.yandex.market.utils.asExceptional

class SmartShoppingRepositoryTest {

    private val coinsMapper = mock<SmartCoinsCollectionMapper>()
    private val userCoinsFapiClient = mock<UserCoinsFapiClient>()
    private val networkingScheduler = NetworkingScheduler(Schedulers.trampoline())
    private val authenticationRepository = mock<AuthRepositoryImpl>()
    private val eventBus = mock<EventBus> {
        on { allEventsStream } doReturn Observable.never()
    }
    private val coinsCountMapper = mock<SmartCoinCountMapper>()
    private val frontApiDataSource = mock<FrontApiDataSource>()
    private val smartShoppingPreferencesRepository = mock<SmartShoppingPreferencesRepository>()

    init {
        whenever(eventBus.allEventsStream).thenReturn(Observable.never())
    }

    private val smartShoppingCacheDataStore = mock<SmartShoppingLocalDataStore>()

    private val bindCoinsJobExecutor = mock<BindCoinsJobExecutor>()
    private val getAuthTokenUseCase = mock<GetAuthTokenUseCase>()
    private val getUuidUseCase = mock<GetUuidUseCase>()
    private val selectedRegionRepository = mock<SelectedRegionRepository>()

    private val repository = SmartShoppingRepository(
        coinsMapper,
        coinsCountMapper,
        networkingScheduler,
        authenticationRepository,
        eventBus,
        frontApiDataSource,
        smartShoppingCacheDataStore,
        userCoinsFapiClient,
        CanGetBonusForPromoJobExecutor(frontApiDataSource),
        bindCoinsJobExecutor,
        getAuthTokenUseCase,
        getUuidUseCase,
        smartShoppingPreferencesRepository,
        selectedRegionRepository,
        mock()
    )

    @Test
    fun `Reloads smart coins when account changes for unauthorized user`() {
        val accountsStream = PublishSubject.create<Optional<UserAccount>>()
        whenever(authenticationRepository.getCurrentAccountStream()) doReturn accountsStream
        whenever(userCoinsFapiClient.getUserCoins()) doReturn Single.just(mergedUserBonusFapiDtoTestInstance())
        whenever(authenticationRepository.getIsLoggedInSingle()) doReturn Single.just(false)
        whenever(authenticationRepository.getLastAccountId()) doReturn Single.just(Optional.empty())
        val collection = SmartCoinsCollection.testBuilder().build()
        whenever(coinsMapper.map(any())) doReturn collection.asExceptional()
        whenever(eventBus.allEventsStream) doReturn Observable.never()
        whenever(smartShoppingCacheDataStore.viewShownEventStream()) doReturn Observable.never()
        whenever(selectedRegionRepository.getSelectedRegionId()) doReturn OptionalLong.empty()

        val observer = repository.getCoinsCollectionStream().test()
        accountsStream.onNext(Optional.empty())

        observer.assertValues(Synchronizing(), Synchronized(collection))
    }

    @Test
    fun `Reloads smart coins after checkout closed unauthorized user`() {
        whenever(authenticationRepository.getCurrentAccountStream()) doReturn Observable.never()
        whenever(userCoinsFapiClient.getUserCoins()) doReturn Single.just(mergedUserBonusFapiDtoTestInstance())
        whenever(authenticationRepository.getIsLoggedInSingle()) doReturn Single.just(false)
        whenever(authenticationRepository.getLastAccountId()) doReturn Single.just(Optional.empty())
        val collection = SmartCoinsCollection.testBuilder().build()
        whenever(coinsMapper.map(any())) doReturn collection.asExceptional()
        val eventsSubject = PublishSubject.create<ApplicationEvent>()
        whenever(eventBus.allEventsStream) doReturn eventsSubject
        whenever(smartShoppingCacheDataStore.viewShownEventStream()) doReturn Observable.never()
        whenever(selectedRegionRepository.getSelectedRegionId()) doReturn OptionalLong.empty()

        val observer = repository.getCoinsCollectionStream().test()
        eventsSubject.onNext(CheckoutTaskClosedEvent)

        observer.assertValues(Synchronizing(), Synchronized(collection))
    }
}