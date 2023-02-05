package flex.actions

import flex.actions.navigation.action.OrderDetailsNavigationAction
import flex.actions.navigation.handler.OrderDetailsNavigationActionHandler
import flex.engine.model.DocumentContext
import org.junit.Test
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class OrderDetailsNavigationActionTest {
    private val action = OrderDetailsNavigationAction(ORDER_ID, false)
    private val context = mock<DocumentContext>()
    private val actionRouter = mock<ActionRouter>()
    private val actionHandler = OrderDetailsNavigationActionHandler(actionRouter)

    @Test
    fun `Check open order details screen action handler`() {
        actionHandler.handle(action, context)

        verify(actionRouter).showOrderDetailsByOrderId(eq(ORDER_ID.toLong()), eq(false), argThat { true })
    }

    companion object {
        private const val ORDER_ID = "1234"
    }
}
