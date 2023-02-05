package ru.yandex.market.clean.presentation.feature.search.searchitem

import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.yandex.market.analytics.facades.OutOfStockAnalogsAnalytics
import ru.yandex.market.analytics.facades.ServicesAnalyticsFacade
import ru.yandex.market.analytics.facades.SuperHypeGoodCarouselAnalyticsFacade
import ru.yandex.market.analytics.facades.pharma.PharmaBookingAnalytics
import ru.yandex.market.analytics.model.ExtraSnippetAnalyticsParams
import ru.yandex.market.analytics.offer.OfferAnalyticsFacade
import ru.yandex.market.clean.domain.model.OfferSpecificationInternal
import ru.yandex.market.clean.domain.model.SearchProductItem
import ru.yandex.market.clean.domain.model.productOfferTestInstance
import ru.yandex.market.clean.domain.model.purchasebylist.MedicineAggregateOfferInfo
import ru.yandex.market.clean.presentation.feature.search.searchitem.realtime.SearchItemRealtimeSignalDelegate
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.clean.presentation.navigation.Screen
import ru.yandex.market.common.featureconfigs.models.PharmaBookingConfig
import ru.yandex.market.net.sku.fapi.dto.specs.SpecificationInternalDto
import ru.yandex.market.presentationSchedulersMock
import ru.yandex.market.test.extensions.asSingle

class SearchItemPresenterTest {

    private val servicesAnalyticsFacade = mock<ServicesAnalyticsFacade>()

    private val offerAnalyticsFacade = mock<OfferAnalyticsFacade>()

    private val outOfStockAnalogsAnalytic = mock<OutOfStockAnalogsAnalytics>()

    private val superHypeGoodCarouselAnalyticsFacade = mock<SuperHypeGoodCarouselAnalyticsFacade>()

    private val searchProductItemOffer = mock<SearchProductItem.Offer> {
        on { productOffer } doReturn productOfferTestInstance().copy(
            medicineAggregateOfferInfo = MedicineAggregateOfferInfo(
                hasBooking = true,
            ),
            internalOfferProperties = OfferSpecificationInternal(
                internals = listOf(SpecificationInternalDto.MEDICINE),
            )
        )
    }

    private val searchItemUseCase = mock<SearchItemUseCase> {
        on { hasHyperlocalAddress() } doReturn Single.just(false)
        on { getPurchaseByListBookingConfig() } doReturn PharmaBookingConfig(true, null).asSingle()
        on { isResaleEnabled() } doReturn false.asSingle()
        on { getRelatedSkuUseCase(any()) } doReturn Single.just("")
    }

    private val arguments = mock<SearchItemPresenter.Arguments> {
        on { searchProductItem } doReturn searchProductItemOffer
    }

    private val router = mock<Router> {
        on { currentScreen } doReturn CURRENT_SCREEN
    }

    private val realtimeSignalDelegate = mock<SearchItemRealtimeSignalDelegate>()

    private val extraSnippetAnalyticsParams = mock<ExtraSnippetAnalyticsParams>()

    private val pharmaBookingAnalytic = mock<PharmaBookingAnalytics>()

    private val analyticsService = mock<ru.yandex.market.analitycs.AnalyticsService>()

    private val presenter = SearchItemPresenter(
        arguments = arguments,
        schedulers = presentationSchedulersMock(),
        router = router,
        useCase = searchItemUseCase,
        servicesAnalyticsFacade = servicesAnalyticsFacade,
        offerAnalyticsFacade = offerAnalyticsFacade,
        pharmaBookingAnalytic = pharmaBookingAnalytic,
        sponsoredSearchMimicAnalyticsFacade = mock(),
        photoFormatter = mock(),
        searchItemViewStateFormatter = mock(),
        errorVoFormatter = mock(),
        sponsoredProductCardMimicIncutFeatureManager = mock(),
        extraSnippetAnalyticsParams = extraSnippetAnalyticsParams,
        realtimeSignalDelegate = realtimeSignalDelegate,
        outOfStockAnalogsAnalytic = outOfStockAnalogsAnalytic,
        superHypeGoodCarouselAnalyticsFacade = superHypeGoodCarouselAnalyticsFacade,
        analyticsService = analyticsService,
    )


    @Test
    fun `Test search item shown event`() {
        presenter.notifySearchItemShown()
        verify(offerAnalyticsFacade).searchItemShowedEvent(
            analyticsParameters = arguments.analyticsParameters,
            offer = searchProductItemOffer.productOffer,
            itemIndex = arguments.position,
            screen = router.currentScreen,
            extraSnippetAnalyticsParams = extraSnippetAnalyticsParams,
            hasHyperlocalAddress = false,
            isShopInShop = false,
            isExclusive = true,
            visualSearchRelatedSkuId = "",
        )
        verify(pharmaBookingAnalytic).bookingInfoVisible(
            showUid = searchProductItemOffer.productOffer.showUid
        )
    }

    companion object {
        private val CURRENT_SCREEN = Screen.HOME
    }
}
