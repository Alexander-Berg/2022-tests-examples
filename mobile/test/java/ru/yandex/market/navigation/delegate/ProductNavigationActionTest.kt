package ru.yandex.market.navigation.delegate

import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.yandex.market.activity.model.SkuTargetScreen
import ru.yandex.market.clean.presentation.feature.product.ProductFragment
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.clean.presentation.navigation.RouterFactory
import ru.yandex.market.clean.presentation.navigation.Screen
import ru.yandex.market.domain.product.model.SkuId

class ProductNavigationActionTest {

    private val router = mock<Router> {
        on { currentScreen } doReturn Screen.ALL_ORDERS
    }

    private val routerFactory = mock<RouterFactory> {
        on { create(any()) } doReturn router
    }

    private val actionRouter = ActionRouterGenerator().generate(
        routerFactory = routerFactory,
    )

    @Test
    fun `Check open product`() {
        actionRouter.openProduct(
            skuId = SKU_ID,
            modelId = MODEL_ID,
            offerId = OFFER_ID,
            offerCpc = OFFER_CPC,
            promoCartDiscountHash = PROMO_CARD_DISCOUNT_HASH
        )

        val screen = SkuTargetScreen(
            ProductFragment.Arguments(
                productId = SkuId(SKU_ID, OFFER_ID, MODEL_ID),
                offerCpc = OFFER_CPC,
                promoCartDiscountHash = PROMO_CARD_DISCOUNT_HASH
            )
        )

        verify(router).navigateTo(screen)
    }

    companion object {
        private const val SKU_ID = "12"
        private const val MODEL_ID = "123"
        private const val OFFER_ID = "1234"
        private const val OFFER_CPC = "12345"
        private const val PROMO_CARD_DISCOUNT_HASH = "123456"
    }
}
