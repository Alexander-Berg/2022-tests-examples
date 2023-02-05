package ru.yandex.market.clean.presentation.feature.pricedrop.cart

import io.reactivex.Observable
import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.isA
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.clean.domain.mapper.CartItemSnapshotDomainMapper
import ru.yandex.market.clean.domain.model.CartItemSnapshot
import ru.yandex.market.clean.domain.model.PriceDropResult
import ru.yandex.market.clean.domain.model.PriceDropResultType
import ru.yandex.market.clean.domain.model.ProductOffer
import ru.yandex.market.clean.domain.model.cartItemSnapshotTestInstance
import ru.yandex.market.clean.domain.model.offerTestInstance
import ru.yandex.market.clean.domain.model.productOfferTestInstance
import ru.yandex.market.clean.presentation.feature.pricedrop.PriceDropOffersTargetScreen
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.clean.presentation.navigation.Screen
import ru.yandex.market.domain.paging.model.PageableResult
import ru.yandex.market.presentationSchedulersMock
import ru.yandex.market.realtimesignal.RealtimeSignalTransport

class CartPriceDropPresenterTest {

    private val useCases = mock<CartPriceDropUseCases> {
        on { prefetchPriceDropForOffer(any(), any()) } doReturn Single.just(defaultTestPriceDrop().toPriceDropResult())
        on {
            prefetchAllPriceDrop(
                any(),
                any()
            )
        } doReturn Single.just(defaultTestPriceDrop().toPriceDropResult() to getCartSnapShotList())
    }
    private val cartPriceDropFlow = mock<CartPriceDropFlow>() {
        on { getCartPriceDropStream() } doReturn Observable.just(cartPriceDropItemTestInstance())
    }
    private val schedulers = presentationSchedulersMock()
    private val cartSnapShotMapper = CartItemSnapshotDomainMapper()
    private val router = mock<Router> {
        on { currentScreen } doReturn Screen.SKU
    }
    private val realtimeSignalTransport = mock<RealtimeSignalTransport>()

    private val view = mock<CartPriceDropView>()

    private val presenter = CartPriceDropPresenter(
        schedulers = schedulers,
        cartPriceDropFlow = cartPriceDropFlow,
        useCases = useCases,
        snapshotMapper = cartSnapShotMapper,
        router = router,
        realtimeSignalTransport = { realtimeSignalTransport },
    )

    @Test
    fun `Check prefetch pricedrop load`() {
        val targetOffer = productOfferTestInstance()
        whenever(useCases.isPriceDropEnabled()) doReturn Single.just(true)

        presenter.attachView(view)
        presenter.setTargetOfferAndPrefetchPricedrop(targetOffer)

        verify(useCases, times(1)).prefetchPriceDropForOffer(targetOffer, targetOffer.minimumCount)
    }

    @Test
    fun `Check only once prefetch happened`() {
        val targetOffer = productOfferTestInstance()
        whenever(useCases.isPriceDropEnabled()) doReturn Single.just(true)

        presenter.attachView(view)
        presenter.setTargetOfferAndPrefetchPricedrop(targetOffer)
        presenter.setTargetOfferAndPrefetchPricedrop(targetOffer)

        verify(useCases, times(1)).prefetchPriceDropForOffer(targetOffer, targetOffer.minimumCount)
    }

    @Test
    fun `Check no prefetch when no target offer set`() {
        whenever(useCases.isPriceDropEnabled()) doReturn Single.just(true)

        presenter.attachView(view)

        verify(useCases, never()).prefetchPriceDropForOffer(any(), any())
    }

    @Test
    fun `Check no prefetch when pricedrop toggle disabled`() {
        val targetOffer = productOfferTestInstance()
        whenever(useCases.isPriceDropEnabled()) doReturn Single.just(false)

        presenter.attachView(view)
        presenter.setTargetOfferAndPrefetchPricedrop(targetOffer)

        verify(useCases, never()).prefetchPriceDropForOffer(any(), any())
    }

    @Test
    fun `Check no full cart price drop request when target offer has no pricedrop`() {
        val targetOffer = productOfferTestInstance()
        whenever(useCases.isPriceDropEnabled()) doReturn Single.just(true)
        whenever(useCases.isPriceDropGrantedByOffer(any())) doReturn Single.just(false)

        presenter.attachView(view)
        presenter.setTargetOfferAndPrefetchPricedrop(targetOffer)

        verify(useCases, never()).prefetchAllPriceDrop(any(), any())
    }

    @Test
    fun `Check price drop open when all conditions met`() {
        val targetOffer = productOfferTestInstance(
            offer = offerTestInstance(persistentId = "persistentOfferId")
        )
        whenever(useCases.isPriceDropEnabled()) doReturn Single.just(true)
        whenever(useCases.isPriceDropGrantedByOffer(any())) doReturn Single.just(true)
        whenever(useCases.filterItemsInCart(any(), any())) doReturn Single.just(getPriceDropOfferList())

        presenter.targetOffer = targetOffer
        presenter.attachView(view)
        presenter.setTargetOfferAndPrefetchPricedrop(targetOffer)

        verify(router).navigateTo(isA<PriceDropOffersTargetScreen>())

    }

    private fun defaultTestPriceDrop() = listOf(productOfferTestInstance())

    private fun List<ProductOffer>.toPriceDropResult(
        pageNumber: Int = 1,
        totalPagesCount: Int = 10
    ): PriceDropResult {
        val pageableResult = PageableResult.builder<ProductOffer>()
            .totalItemsCount(totalPagesCount * size)
            .totalPagesCount(totalPagesCount)
            .requestedItemsCount(size)
            .currentPageIndex(pageNumber)
            .data(this)
            .build()
        return PriceDropResult(pageableResult, PriceDropResultType.PriceDrop)
    }

    private fun getCartSnapShotList(): List<CartItemSnapshot> {
        val firstItem = cartItemSnapshotTestInstance(
            persistentOfferId = "persistentOfferFirst",
            stockKeepingUnitId = "stockKeepingIdFirst"
        )
        val secondItem = cartItemSnapshotTestInstance(
            persistentOfferId = "persistentOfferSecond",
            stockKeepingUnitId = "stockKeepingIdSecond"
        )
        return listOf(firstItem, secondItem)
    }

    private fun getPriceDropOfferList(): List<ProductOffer> {
        val firstItem = productOfferTestInstance()
        val secondItem = productOfferTestInstance()
        return listOf(firstItem, secondItem)
    }

}