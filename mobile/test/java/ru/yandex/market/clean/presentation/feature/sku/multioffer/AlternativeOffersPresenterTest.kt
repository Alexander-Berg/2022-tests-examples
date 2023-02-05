package ru.yandex.market.clean.presentation.feature.sku.multioffer

import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.yandex.market.analitycs.events.operationalrating.RatingAnalyticsParams
import ru.yandex.market.analytics.offer.AlternativeOffersAnalyticsFacade
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.clean.presentation.navigation.Screen
import ru.yandex.market.presentationSchedulersMock

class AlternativeOffersPresenterTest {

    private val router = mock<Router> {
        on { currentScreen } doReturn CURRENT_SCREEN
    }

    private val useCases = mock<AlternativeOffersUseCases>()

    private val alternativeOffersAnalyticsFacade = mock<AlternativeOffersAnalyticsFacade>()

    val presenter = AlternativeOffersPresenter(
        schedulers = presentationSchedulersMock(),
        router = router,
        useCases = useCases,
        alternativeOfferFormatter = mock(),
        operationalRatingFormatter = mock(),
        args = mock(),
        analyticsService = mock(),
        giftBlockShownSender = mock(),
        alternativeOffersAnalyticsFacade = alternativeOffersAnalyticsFacade,
        serviceFormatter = mock(),
    )

    @Test
    fun `Test Alternative Offer Item Visible Event`() {
        val multiOfferAnalyticsParam = mock<MultiOfferAnalyticsParam>()
        val supplierRatingAnalyticsParam = mock<RatingAnalyticsParams>()
        presenter.onAlternativeOfferShow(multiOfferAnalyticsParam, supplierRatingAnalyticsParam)
        verify(alternativeOffersAnalyticsFacade).alternativeOfferItemVisibleEvent(
            false,
            multiOfferAnalyticsParam,
            router.currentScreen,
            isExclusive = false,
            isNovice = false,
        )
        verify(alternativeOffersAnalyticsFacade).alternativeOfferSellerRatingShow(supplierRatingAnalyticsParam)
    }

    companion object {
        private val CURRENT_SCREEN = Screen.HOME
    }

}
