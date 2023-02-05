package ru.yandex.market.clean.presentation.feature.wishlist

import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.yandex.market.analytics.offer.OfferAnalyticsFacade
import ru.yandex.market.clean.domain.model.ProductOffer
import ru.yandex.market.clean.presentation.feature.wishlist.fulfillmentitem.FulfillmentVO
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.clean.presentation.navigation.Screen
import ru.yandex.market.domain.product.model.SkuId
import ru.yandex.market.net.sku.SkuType

class WishListPresenterTest {

    private val router = mock<Router> {
        on { currentScreen } doReturn CURRENT_SCREEN
    }
    private val offerAnalyticsFacade = mock<OfferAnalyticsFacade>()

    val presenter = WishListPresenter(
        mock(),
        router,
        mock(),
        mock(),
        mock(),
        mock(),
        mock(),
        mock(),
        offerAnalyticsFacade,
        mock(),
        mock(),
        mock()
    )

    @Test
    fun `WishList Snippet Visible Event`() {
        val offer = mock<ProductOffer>()
        val vo = mock<FulfillmentVO>() {
            on { isNoStock } doReturn false
            on { productOffer } doReturn offer
        }
        presenter.onItemVisible(vo)
        verify(offerAnalyticsFacade).wishListSnippetVisibleEvent(CURRENT_SCREEN, offer)
    }

    @Test
    fun `WishList Snippet Navigate Event`() {
        val skuId = mock<SkuId>() {
            on { id } doReturn "id"
        }
        val offer = mock<ProductOffer>()
        val vo = mock<FulfillmentVO>() {
            on { isNoStock } doReturn false
            on { modelId } doReturn "id"
            on { skuType } doReturn SkuType.UNKNOWN
            on { productId } doReturn skuId
            on { productOffer } doReturn offer
        }

        presenter.onProductItemClick(vo)

        verify(offerAnalyticsFacade).wishListSnippetNavigateEvent(CURRENT_SCREEN, skuId, SkuType.UNKNOWN, offer)
    }

    companion object {
        private val CURRENT_SCREEN = Screen.HOME
    }
}
