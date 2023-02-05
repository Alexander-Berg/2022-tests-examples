package flex.actions

import flex.actions.navigation.action.PickupPointNavigationAction
import flex.actions.navigation.handler.PickupPointNavigationActionHandler
import flex.engine.model.DocumentContext
import org.junit.Test
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class PickupPointNavigationActionTest {
    private val action = PickupPointNavigationAction(ORDER_ID, false)
    private val context = mock<DocumentContext>()
    private val actionRouter = mock<ActionRouter>()
    private val actionHandler = PickupPointNavigationActionHandler(actionRouter)

    @Test
    fun `Check change order prepayment navigation action handler`() {
        actionHandler.handle(action, context)

        verify(actionRouter).showPickupPointByOrderId(eq(ORDER_ID.toLong()), eq(false), argThat { true })
    }

    companion object {
        private const val ORDER_ID = "1234"
    }
}
