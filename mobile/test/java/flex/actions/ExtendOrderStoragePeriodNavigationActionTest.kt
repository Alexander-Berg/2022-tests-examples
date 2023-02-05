package flex.actions

import flex.actions.navigation.action.ExtendOrderStoragePeriodNavigationAction
import flex.actions.navigation.handler.ExtendOrderStoragePeriodNavigationActionHandler
import flex.engine.model.DocumentContext
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class ExtendOrderStoragePeriodNavigationActionTest {
    private val action = ExtendOrderStoragePeriodNavigationAction(ORDER_ID)
    private val context = mock<DocumentContext>()
    private val actionRouter = mock<ActionRouter>()
    private val actionHandler = ExtendOrderStoragePeriodNavigationActionHandler(actionRouter)

    @Test
    fun `Check extend storage period action handler`() {
        actionHandler.handle(action, context)

        verify(actionRouter).extendPeriod(action.orderId.toLong())
    }

    companion object {
        private const val ORDER_ID = "1234"
    }
}
