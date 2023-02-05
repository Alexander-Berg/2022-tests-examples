package ru.yandex.market.clean.presentation.feature.comparisonlists.comparison

import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.yandex.market.analytics.facades.OfferEventData
import ru.yandex.market.analytics.offer.OfferAnalyticsFacade
import ru.yandex.market.clean.domain.model.comparisons.ComparisonProduct
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.clean.presentation.navigation.Screen
import ru.yandex.market.domain.product.model.SkuId

class ComparisonPresenterTest {

    private val offerAnalyticsFacade = mock<OfferAnalyticsFacade>()
    private val args = mock<ComparisonFragment.Arguments>() {
        on { categoryId } doReturn "id"
    }

    private val router = mock<Router>() {
        on { currentScreen } doReturn CURRENT_SCREEN
    }

    val presenter = ComparisonPresenter(
        router,
        mock(),
        mock(),
        mock(),
        mock(),
        offerAnalyticsFacade,
        args,
        mock(),
    )

    @Test
    fun `Test comparison Main Screen Snippet Navigate Event`() {
        val skuId = mock<SkuId>() {
            on { id } doReturn "id"
        }
        val offerEventData = mock<OfferEventData>()

        val product = mock<ComparisonProduct>() {
            on { id } doReturn skuId
        }
        presenter.comparisonProducts = mutableListOf(product)
        presenter.navigateToProduct(skuId, null, "", offerEventData)
        verify(offerAnalyticsFacade).comparisonMainScreenSnippetNavigateEvent(
            0,
            "id",
            "id",
            offerEventData,
            null,
            CURRENT_SCREEN
        )
    }

    companion object {
        private val CURRENT_SCREEN = Screen.HOME
    }
}