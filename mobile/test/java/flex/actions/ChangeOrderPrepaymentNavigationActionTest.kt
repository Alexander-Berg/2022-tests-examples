package flex.actions

import flex.actions.navigation.action.ChangeOrderPrepaymentNavigationAction
import flex.actions.navigation.handler.ChangeOrderPrepaymentNavigationActionHandler
import flex.engine.model.DocumentContext
import org.junit.Test
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class ChangeOrderPrepaymentNavigationActionTest {
    private val action = ChangeOrderPrepaymentNavigationAction(ORDER_ID, false)
    private val context = mock<DocumentContext>()
    private val actionRouter = mock<ActionRouter>()
    private val actionHandler = ChangeOrderPrepaymentNavigationActionHandler(actionRouter)

    @Test
    fun `Check change order prepayment navigation action handler`() {
        actionHandler.handle(action, context)

        verify(actionRouter).changePrepaymentByOrderId(
            eq(action.orderId.toLong()),
            eq(action.isArchived),
            argThat { true })
    }

    companion object {
        private const val ORDER_ID = "1234"
    }
}
