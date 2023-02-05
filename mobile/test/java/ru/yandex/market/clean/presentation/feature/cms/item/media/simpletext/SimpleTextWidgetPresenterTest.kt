package ru.yandex.market.clean.presentation.feature.cms.item.media.simpletext

import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.yandex.market.analytics.offer.OfferAnalyticsFacade
import ru.yandex.market.clean.domain.model.cms.AlternativeOffersLinkAnalyticsData
import ru.yandex.market.clean.domain.model.cms.AlternativeOffersLinkArguments
import ru.yandex.market.clean.domain.model.cms.NavigateAction
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.clean.presentation.navigation.Screen

class SimpleTextWidgetPresenterTest {

    private val router = mock<Router> {
        on { currentScreen } doReturn CURRENT_SCREEN
    }

    private val offerAnalyticsFacade = mock<OfferAnalyticsFacade>()

    val presenter = SimpleTextWidgetPresenter(
        mock(),
        mock(),
        mock(),
        mock(),
        mock(),
        router,
        mock(),
        offerAnalyticsFacade,
        mock()
    )

    @Test
    fun `Alternative offer navigate event`() {
        val analyticsData = mock<AlternativeOffersLinkAnalyticsData>()
        val action = NavigateAction(AlternativeOffersLinkArguments("", "", "", 0, "", analyticsData))
        presenter.sendAlternativeOfferNavigateAnalyticsIfNeeded(action)
        verify(offerAnalyticsFacade).allAlternativeOffersNavigateEvent(analyticsData, CURRENT_SCREEN)
    }

    companion object {
        private val CURRENT_SCREEN = Screen.HOME
    }
}