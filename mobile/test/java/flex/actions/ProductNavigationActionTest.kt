package flex.actions

import flex.actions.navigation.action.ProductNavigationAction
import flex.actions.navigation.handler.ProductNavigationActionHandler
import flex.engine.model.DocumentContext
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class ProductNavigationActionTest {
    private val action = ProductNavigationAction(
        SKU_ID,
        MODEL_ID,
        OFFER_ID,
        OFFER_CPC,
        PROMO_CARD_DISCOUNT_HASH,
    )
    private val context = mock<DocumentContext>()
    private val actionRouter = mock<ActionRouter>()
    private val actionHandler = ProductNavigationActionHandler(actionRouter)

    @Test
    fun `Check open product action handler`() {
        actionHandler.handle(action, context)

        verify(actionRouter).openProduct(
            SKU_ID,
            MODEL_ID,
            OFFER_ID,
            OFFER_CPC,
            PROMO_CARD_DISCOUNT_HASH
        )
    }

    companion object {
        private const val SKU_ID = "12"
        private const val MODEL_ID = "123"
        private const val OFFER_ID = "1234"
        private const val OFFER_CPC = "12345"
        private const val PROMO_CARD_DISCOUNT_HASH = "123456"
    }
}
