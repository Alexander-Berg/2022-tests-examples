package ru.yandex.market.clean.presentation.feature.pricedrop

import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.TestScheduler
import io.reactivex.subjects.PublishSubject
import org.junit.Ignore
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.analitycs.events.health.HealthEvent
import ru.yandex.market.analitycs.events.health.additionalData.PriceDropOffersFilteredInfo
import ru.yandex.market.analytics.health.HealthLevel
import ru.yandex.market.analytics.health.HealthName
import ru.yandex.market.analytics.health.HealthPortion
import ru.yandex.market.analytics.mapper.PriceDropAnalyticsDjPlaceMapper
import ru.yandex.market.clean.domain.model.PriceDropResult
import ru.yandex.market.clean.domain.model.PriceDropResultType
import ru.yandex.market.clean.domain.model.ProductOffer
import ru.yandex.market.clean.domain.model.cartItemSnapshotTestInstance
import ru.yandex.market.clean.domain.model.offerTestInstance
import ru.yandex.market.clean.domain.model.priceDropInfoTestInstance
import ru.yandex.market.clean.domain.model.productOfferTestInstance
import ru.yandex.market.clean.presentation.formatter.ProductOfferFormatter
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.clean.presentation.navigation.Screen
import ru.yandex.market.common.errors.ErrorVo
import ru.yandex.market.common.errors.ErrorVoFormatter
import ru.yandex.market.common.errors.MetricErrorInfoMapper
import ru.yandex.market.common.errors.metricErrorInfoTestInstance
import ru.yandex.market.common.experiments.experiment.sponsored.SponsoredTagNameExperiment
import ru.yandex.market.domain.paging.model.PageableResult
import ru.yandex.market.feature.manager.CmsSearchSnippetFeatureManager
import ru.yandex.market.feature.manager.SponsoredTagNameFeatureManager
import ru.yandex.market.internal.ConnectivityStatusController
import ru.yandex.market.presentationSchedulersMock
import ru.yandex.market.test.extensions.arg
import ru.yandex.market.test.extensions.asSingle
import ru.yandex.market.test.extensions.asStream
import ru.yandex.market.ui.view.mvp.cartcounterbutton.OffersCache
import ru.yandex.market.utils.advanceTimeBy
import ru.yandex.market.utils.millis
import ru.yandex.market.utils.partitionIndexed
import ru.yandex.market.utils.seconds

@Ignore
class PriceDropOffersPresenterTest {

    private val schedulers = presentationSchedulersMock()
    private val priceDropHeaderVo = priceDropHeaderVoTestInstance()
    private val priceDropInfo = priceDropInfoTestInstance()
    private val useCases = mock<PriceDropOffersUseCases> {
        on { getCartItemsForPriceDrop() } doReturn Single.just(listOf(cartItemSnapshotTestInstance()))
        on { getPriceDropInfo(PriceDropResultType.PriceDrop) } doReturn Single.just(priceDropInfo)
        on { isLoggedIn() } doReturn Single.just(false)
        on { getCachedCartSnapShot() } doReturn Single.just(listOf(cartItemSnapshotTestInstance()))
    }
    private val offersFormatter = mock<ProductOfferFormatter> {
        on { format(any(), any(), any(), any(), any(), any()) } doReturn productOfferVoTestInstance()
    }
    private val priceDropInfoFormatter = mock<PriceDropInfoFormatter> {
        on { format(priceDropInfo) } doReturn priceDropHeaderVo
    }
    private val errorVoFormatter = mock<ErrorVoFormatter>()
    private val router = mock<Router> {
        on { currentScreen } doReturn Screen.SKU
    }
    private val view = mock<PriceDropOffersView>()
    private val configuration = spy(
        PriceDropOffersPresenter.Configuration(
            pageSize = 4,
            retryDelay = 0.millis,
            columnsCount = 3,
            visibleSkeletonRows = 1,
            showNetworkErrorDuration = 3.seconds
        )
    )
    private val connectivityStatus = mock<ConnectivityStatusController> {
        on { networkAvailabilityStream } doReturn Observable.never()
    }
    private val cmsSearchSnippetFeatureManager = mock<CmsSearchSnippetFeatureManager> {
        on { isEnabled() } doReturn Single.just(true)
    }
    private val sponsoredTagNameFeatureManager = mock<SponsoredTagNameFeatureManager> {
        on { getSponsoredNaming() } doReturn SponsoredTagNameExperiment.SponsoredNaming.SPONSORED
    }

    @Suppress("DEPRECATION")
    private val analyticsService = mock<ru.yandex.market.analitycs.AnalyticsService>()
    private val cartCounterArgumentsBridge = mock<OffersCache>()
    private val priceDropArguments = mock<PriceDropArguments>()
    private val metricErrorInfoMapper = mock<MetricErrorInfoMapper>()
    private val priceDropAnalyticsDjPlaceMapper = mock<PriceDropAnalyticsDjPlaceMapper>()
    private val presenter = PriceDropOffersPresenter(
        schedulers,
        useCases,
        configuration,
        router,
        connectivityStatus,
        analyticsService,
        cartCounterArgumentsBridge,
        priceDropInfoFormatter,
        priceDropArguments,
        errorVoFormatter,
        metricErrorInfoMapper,
        cmsSearchSnippetFeatureManager,
        sponsoredTagNameFeatureManager,
        priceDropAnalyticsDjPlaceMapper,
    )

    @Test
    fun `Show info header on view attach`() {
        val offers = getOffers().toPriceDropResult()
        whenever(useCases.getOffers(any(), any(), any())) doReturn offers.asSingle()
        presenter.attachView(view)
        verify(view).showHeader(priceDropHeaderVo)
    }

    @Test
    fun `Fill skeletons to full row during page load`() {
        whenever(useCases.getOffers(any(), any(), any()))
            .thenReturn(
                Single.just(
                    PageableResult.builder<ProductOffer>()
                        .totalItemsCount(1)
                        .totalPagesCount(3)
                        .requestedItemsCount(4)
                        .currentPageIndex(1)
                        .data(listOf(productOfferTestInstance()))
                        .build()
                        .toPriceDropResult()
                )
            )

        presenter.attachView(view)
        presenter.loadMoreItems()

        verify(view).addItemSkeletons(5)
    }

    @Test
    fun `Show specified skeleton rows`() {
        whenever(useCases.getOffers(any(), any(), any()))
            .thenReturn(
                Single.just(
                    PageableResult.builder<ProductOffer>()
                        .totalItemsCount(3)
                        .totalPagesCount(2)
                        .requestedItemsCount(3)
                        .currentPageIndex(1)
                        .data((0..2).map { productOfferTestInstance() })
                        .build()
                        .toPriceDropResult()
                )
            )

        presenter.attachView(view)
        presenter.loadMoreItems()

        verify(view).addItemSkeletons(configuration.columnsCount * configuration.visibleSkeletonRows)
    }

    @Test
    fun `Do not show offers already in cart when loading initial page`() {
        val offers = getOffers(pageNumber = 1)
        whenever(useCases.getOffers(any(), any(), any())).thenReturn(Single.just(offers.toPriceDropResult()))
        val (inCart, notInCart) = offers.skuIds.partitionIndexed { index, _ -> index % 2 == 0 }
        whenever(offersFormatter.format(any(), any(), any(), any(), any(), any())).thenAnswer {
            val skuId = requireNotNull((it.getArgument(0) as ProductOffer).stockKeepingUnitId)
            productOfferVoTestInstance(skuId = skuId)
        }

        presenter.attachView(view)

        verify(view).showNewItems(
            argThat { map { it.stockKeepingUnitId } == notInCart },
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
        )
    }

    @Test
    fun `Load next page when there is no items after filtering initial page`() {
        val firstPageOffers = getOffers(pageNumber = 1)
        val secondPageOffers = getOffers(pageNumber = 2)
        whenever(useCases.getOffers(any(), any(), any()))
            .thenReturn(
                Single.just(firstPageOffers.toPriceDropResult(pageNumber = 1)),
                Single.just(secondPageOffers.toPriceDropResult(pageNumber = 2))
            )
        whenever(offersFormatter.format(any(), any(), any(), any(), any(), any())).thenAnswer {
            val skuId = requireNotNull((it.getArgument(0) as ProductOffer).stockKeepingUnitId)
            productOfferVoTestInstance(skuId = skuId)
        }

        presenter.attachView(view)

        verify(analyticsService).report(emptyOffersEvent(cartSkusCount = firstPageOffers.size))
        verify(view).showNewItems(argThat {
            map { it.stockKeepingUnitId } == secondPageOffers.skuIds
        }, any(), any(), any(), any(), any(), any())
    }

    @Test
    fun `Do not show offers already in cart when loading pages`() {
        val firstPageOffers = getOffers(pageNumber = 1)
        val secondPageOffers = getOffers(pageNumber = 2)
        whenever(useCases.getOffers(any(), any(), any()))
            .thenReturn(
                Single.just(firstPageOffers.toPriceDropResult(pageNumber = 1)),
                Single.just(secondPageOffers.toPriceDropResult(pageNumber = 2))
            )
        val (inCart, notInCart) = secondPageOffers.skuIds
            .partitionIndexed { index, _ -> index % 2 == 0 }
        whenever(offersFormatter.format(any(), any(), any(), any(), any(), any())).thenAnswer {
            val skuId = requireNotNull((it.getArgument(0) as ProductOffer).stockKeepingUnitId)
            productOfferVoTestInstance(skuId = skuId)
        }

        presenter.attachView(view)
        presenter.loadMoreItems()

        verify(view).replaceSkeletonsWithActualItems(
            any(),
            argThat { map { it.stockKeepingUnitId } == notInCart },
            any(),
            any(),
            any(),
            any(),
            any(),
        )
    }

    @Test
    fun `Load next page when there is no items after filtering page`() {
        val firstPageOffers = getOffers(pageNumber = 1)
        val secondPageOffers = getOffers(pageNumber = 2)
        val thirdPageOffers = getOffers(pageNumber = 3)
        whenever(useCases.getOffers(any(), any(), any()))
            .thenReturn(
                Single.just(firstPageOffers.toPriceDropResult(pageNumber = 1)),
                Single.just(secondPageOffers.toPriceDropResult(pageNumber = 2)),
                Single.just(thirdPageOffers.toPriceDropResult(pageNumber = 3))
            )

        whenever(offersFormatter.format(any(), any(), any(), any(), any(), any())).thenAnswer {
            val skuId = requireNotNull((it.getArgument(0) as ProductOffer).stockKeepingUnitId)
            productOfferVoTestInstance(skuId = skuId)
        }

        presenter.attachView(view)
        presenter.loadMoreItems()

        verify(analyticsService).report(emptyOffersEvent(pageNumber = 2, cartSkusCount = secondPageOffers.size))
        verify(view).replaceSkeletonsWithActualItems(
            any(),
            argThat { map { it.stockKeepingUnitId } == thirdPageOffers.skuIds },
            any(),
            any(),
            any(),
            any(),
            any(),
        )
    }

    @Test
    fun `Hide skeletons when there is no items after filtering page and there is no more pages left`() {
        val firstPageOffers = getOffers(pageNumber = 1)
        val secondPageOffers = getOffers(pageNumber = 2)
        whenever(useCases.getOffers(any(), any(), any()))
            .thenReturn(
                Single.just(firstPageOffers.toPriceDropResult(pageNumber = 1)),
                Single.just(secondPageOffers.toPriceDropResult(pageNumber = 2, totalPagesCount = 2))
            )

        whenever(offersFormatter.format(any(), any(), any(), any(), any(), any())).thenAnswer {
            val skuId = requireNotNull((it.getArgument(0) as ProductOffer).stockKeepingUnitId)
            productOfferVoTestInstance(skuId = skuId)
        }

        presenter.attachView(view)
        presenter.loadMoreItems()

        verify(analyticsService).report(
            emptyOffersEvent(pageNumber = 2, cartSkusCount = secondPageOffers.size, totalPagesCount = 2)
        )
        verify(view).replaceSkeletonsWithActualItems(any(), eq(emptyList()), any(), any(), any(), any(), any())
    }

    @Test
    fun `Do not show skeletons second time when skipping page due to filtering`() {
        val firstPageOffers = getOffers(pageNumber = 1)
        val secondPageOffers = getOffers(pageNumber = 2)
        val thirdPageOffers = getOffers(pageNumber = 3)
        whenever(useCases.getOffers(any(), any(), any()))
            .thenReturn(
                Single.just(firstPageOffers.toPriceDropResult(pageNumber = 1)),
                Single.just(secondPageOffers.toPriceDropResult(pageNumber = 2)),
                Single.just(thirdPageOffers.toPriceDropResult(pageNumber = 3))
            )

        whenever(offersFormatter.format(any(), any(), any(), any(), any(), any())).thenAnswer {
            val skuId = requireNotNull((it.getArgument(0) as ProductOffer).stockKeepingUnitId)
            productOfferVoTestInstance(skuId = skuId)
        }
        var skeletonsShown = 0
        whenever(view.addItemSkeletons(any())).thenAnswer {
            skeletonsShown = it.getArgument(0) as Int
            Unit
        }

        presenter.attachView(view)
        presenter.loadMoreItems()

        verify(view).addItemSkeletons(skeletonsShown)
    }

    @Test
    fun `Observes connectivity changes`() {
        val result = getOffers().toPriceDropResult()
        whenever(schedulers.timer).thenReturn(TestScheduler())
        whenever(useCases.getOffers(any(), any(), any())).thenReturn(Single.just(result))
        val connectivityStream = PublishSubject.create<Boolean>()
        whenever(connectivityStatus.networkAvailabilityStream).thenReturn(connectivityStream)
        val errorVo = ErrorVo("Alert", metricErrorInfo = metricErrorInfoTestInstance())
        whenever(errorVoFormatter.format(any() as Int, any(), any(), any(), any())).thenReturn(errorVo)

        presenter.attachView(view)
        connectivityStream.apply {
            onNext(false)
            onNext(true)
            onNext(false)
            onNext(true)
        }

        view.inOrder {
            verify(view).showErrorAlert(errorVo)
            verify(view).hideAlertMessages()
            verify(view).showErrorAlert(errorVo)
            verify(view).hideAlertMessages()
        }
    }

    @Test
    fun `Hide error message after specified delay`() {
        val timerScheduler = TestScheduler()
        whenever(schedulers.timer).thenReturn(timerScheduler)
        val result = getOffers().toPriceDropResult()
        whenever(useCases.getOffers(any(), any(), any())).thenReturn(Single.just(result))
        whenever(connectivityStatus.networkAvailabilityStream).thenReturn(false.asStream())
        val errorVo = ErrorVo("Alert", metricErrorInfo = metricErrorInfoTestInstance())
        whenever(errorVoFormatter.format(any() as Int, any(), any(), any(), any())).thenReturn(errorVo)

        presenter.attachView(view)

        verify(view).showErrorAlert(any())
        verify(view, never()).hideAlertMessages()

        timerScheduler.advanceTimeBy(configuration.showNetworkErrorDuration)
        verify(view).hideAlertMessages()
    }

    @Test
    fun `Fill skeletons to full row after initial page load with filtering`() {
        whenever(configuration.columnsCount).thenReturn(2)
        whenever(configuration.visibleSkeletonRows).thenReturn(1)
        whenever(configuration.pageSize).thenReturn(4)
        val offers = getOffers()
        val inCart = listOf(requireNotNull(offers.first().stockKeepingUnitId))
        whenever(useCases.getOffers(any(), any(), any()))
            .thenReturn(
                Single.just(
                    PageableResult.builder<ProductOffer>()
                        .data(offers)
                        .currentPageIndex(1)
                        .totalPagesCount(2)
                        .requestedItemsCount(4)
                        .totalItemsCount(20)
                        .build()
                        .toPriceDropResult()
                )
            )
        whenever(offersFormatter.format(any(), any(), any(), any(), any(), any())).thenAnswer {
            val skuId = requireNotNull((it.getArgument(0) as ProductOffer).stockKeepingUnitId)
            productOfferVoTestInstance(skuId = skuId)
        }

        presenter.attachView(view)
        presenter.loadMoreItems()

        verify(view).addItemSkeletons(3)
    }

    @Test
    fun `Fill skeletons to full row after second page load with filtering`() {
        whenever(configuration.columnsCount).thenReturn(2)
        whenever(configuration.visibleSkeletonRows).thenReturn(1)
        whenever(configuration.pageSize).thenReturn(4)
        val firstPageOffers = getOffers()
        val secondPageOffers = getOffers(pageNumber = 2)
        val inCart =
            (listOf(firstPageOffers.first()) + secondPageOffers.subList(0, 2)).mapNotNull { it.stockKeepingUnitId }
        whenever(useCases.getOffers(any(), any(), any()))
            .thenReturn(
                Single.just(
                    PageableResult.builder<ProductOffer>()
                        .data(firstPageOffers)
                        .currentPageIndex(1)
                        .totalPagesCount(3)
                        .requestedItemsCount(4)
                        .totalItemsCount(12)
                        .build()
                        .toPriceDropResult()
                ),
                Single.just(
                    PageableResult.builder<ProductOffer>()
                        .data(secondPageOffers)
                        .currentPageIndex(2)
                        .totalPagesCount(3)
                        .requestedItemsCount(4)
                        .totalItemsCount(12)
                        .build()
                        .toPriceDropResult()
                )
            )
        whenever(offersFormatter.format(any(), any(), any(), any(), any(), any())).thenAnswer {
            val skuId = requireNotNull((it.getArgument(0) as ProductOffer).stockKeepingUnitId)
            productOfferVoTestInstance(skuId = skuId)
        }

        presenter.attachView(view)
        presenter.loadMoreItems()
        presenter.loadMoreItems()

        verify(view, times(2)).addItemSkeletons(3)
    }

    @Test
    fun `Properly reload offers after error`() {
        val offers = getOffers().toPriceDropResult()
        val metricErrorInfo = metricErrorInfoTestInstance()
        whenever(useCases.getOffers(any(), any(), any())).doReturn(Single.error(RuntimeException()), offers.asSingle())
        whenever(metricErrorInfoMapper.map(any(), any(), any(), any())).doReturn(metricErrorInfo)
        presenter.attachView(view)
        presenter.reloadOffers()

        view.inOrder {
            verify().setEndlessScrollEnabled(false)
            verify().showInitialProgress()
            verify().showGenericError(metricErrorInfo)
            verify().setEndlessScrollEnabled(false)
            verify().showProgress()
            verify().showNewItems(any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `Do not show internal progress on initial load`() {
        val offers = getOffers().toPriceDropResult()
        whenever(useCases.getOffers(any(), any(), any())) doReturn offers.asSingle()

        presenter.attachView(view)

        verify(view, never()).showProgress()
    }

    @Test
    fun `Do not show already shown skus`() {
        val firstPageOffers = listOf(
            productOfferTestInstance(offer = offerTestInstance(stockKeepingUnitId = "1")),
            productOfferTestInstance(offer = offerTestInstance(stockKeepingUnitId = "2"))
        ).toPriceDropResult()
        val secondPageOffers = listOf(
            productOfferTestInstance(offer = offerTestInstance(stockKeepingUnitId = "1")),
            productOfferTestInstance(offer = offerTestInstance(stockKeepingUnitId = "3"))
        ).toPriceDropResult()
        whenever(useCases.getOffers(any(), any(), any())).doReturn(
            firstPageOffers.asSingle(),
            secondPageOffers.asSingle()
        )
        whenever(offersFormatter.format(any(), any(), any(), any(), any(), any())) doAnswer {
            productOfferVoTestInstance(skuId = requireNotNull(it.arg<ProductOffer>().stockKeepingUnitId))
        }

        presenter.attachView(view)
        presenter.loadMoreItems()

        view.replaceSkeletonsWithActualItems(
            any(),
            argThat { size == 1 && first().stockKeepingUnitId == "3" },
            any(),
            any(),
            any(),
            any(),
            any(),
        )
    }

    @Test
    fun `Push loaded product offers to cart counter arguments bridge`() {
        val pageSize = 10
        whenever(configuration.pageSize) doReturn pageSize
        val firstPageOffers = getOffers()
        val secondPageOffers = getOffers(pageNumber = 2)
        whenever(useCases.getOffers(any(), any(), any()))
            .thenReturn(
                firstPageOffers.toPriceDropResult().asSingle(),
                secondPageOffers.toPriceDropResult(pageNumber = 2).asSingle()
            )

        presenter.attachView(view)
        presenter.loadMoreItems()

        verify(cartCounterArgumentsBridge, times(2)).addOffers(argThat { size == pageSize })
    }

    private fun getOffers(pageNumber: Int = 1): List<ProductOffer> {
        val pageSize = configuration.pageSize
        val startIndex = (pageNumber - 1) * pageSize
        val endIndex = startIndex + pageSize - 1
        return (startIndex..endIndex).map {
            productOfferTestInstance(offer = offerTestInstance(stockKeepingUnitId = it.toString()))
        }
    }

    private fun List<ProductOffer>.toPriceDropResult(
        pageNumber: Int = 1,
        totalPagesCount: Int = DEFAULT_PAGES_COUNT
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

    private fun PageableResult<ProductOffer>.toPriceDropResult(): PriceDropResult {
        return PriceDropResult(this, PriceDropResultType.PriceDrop)
    }

    private val List<ProductOffer>.skuIds get() = mapNotNull { it.stockKeepingUnitId }

    private fun emptyOffersEvent(
        cartSkusCount: Int,
        pageNumber: Int = 1,
        totalPagesCount: Int = DEFAULT_PAGES_COUNT
    ): HealthEvent {
        return HealthEvent.builder()
            .name(HealthName.PRICE_DROP_OFFERS_FILTERED)
            .portion(HealthPortion.PRICE_DROP_POPUP_SCREEN)
            .level(HealthLevel.WARNING)
            .info(
                PriceDropOffersFilteredInfo(
                    pageNumber = pageNumber,
                    requestedItemsCount = configuration.pageSize,
                    cartSkusCount = cartSkusCount,
                    totalPagesCount = totalPagesCount
                )
            )
            .build()
    }

    companion object {
        private const val DEFAULT_PAGES_COUNT = 10
    }
}
