package flex.actions

import flex.actions.navigation.action.TrackCourierNavigationAction
import flex.actions.navigation.handler.TrackCourierNavigationActionHandler
import flex.engine.model.DocumentContext
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class TrackCourierNavigationActionTest {
    private val action = TrackCourierNavigationAction(ORDER_ID)
    private val context = mock<DocumentContext>()
    private val actionRouter = mock<ActionRouter>()
    private val actionHandler = TrackCourierNavigationActionHandler(actionRouter)

    @Test
    fun `Open track courier screen action handler`() {
        actionHandler.handle(action, context)

        verify(actionRouter).showCourierTrackingScreen(ORDER_ID.toLong())
    }

    companion object {
        private const val ORDER_ID = "1234"
    }
}
