package flex.actions

import flex.actions.navigation.action.OnDemandOrderNavigationAction
import flex.actions.navigation.handler.OnDemandOrderNavigationActionHandler
import flex.engine.model.DocumentContext
import org.junit.Test
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class OnDemandOrderNavigationActionTest {
    private val action = OnDemandOrderNavigationAction(ORDER_ID, false)
    private val context = mock<DocumentContext>()
    private val actionRouter = mock<ActionRouter>()
    private val actionHandler = OnDemandOrderNavigationActionHandler(actionRouter)

    @Test
    fun `Check ondemand order navigation action handler`() {
        actionHandler.handle(action, context)

        verify(actionRouter).openOnDemandByOrderId(eq(ORDER_ID.toLong()), eq(false), argThat { true })
    }

    companion object {
        private const val ORDER_ID = "1234"
    }
}
