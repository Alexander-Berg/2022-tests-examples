package flex.actions

import flex.actions.navigation.action.RepeatOrderAction
import flex.actions.navigation.handler.RepeatOrderActionHandler
import flex.engine.model.DocumentContext
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class RepeatOrderActionTest {
    private val action = RepeatOrderAction(ORDER_ID, false)
    private val context = mock<DocumentContext>()
    private val actionRouter = mock<ActionRouter>()
    private val actionHandler = RepeatOrderActionHandler(actionRouter)

    @Test
    fun `Check repeat order action handler`() {
        actionHandler.handle(action, context)

        verify(actionRouter).repeatOrder(ORDER_ID.toLong(), false)
    }

    companion object {
        private const val ORDER_ID = "1234"
    }
}
