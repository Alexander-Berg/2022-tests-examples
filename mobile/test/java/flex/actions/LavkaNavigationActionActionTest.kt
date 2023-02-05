package flex.actions

import flex.actions.navigation.action.LavkaNavigationAction
import flex.actions.navigation.handler.LavkaNavigationActionHandler
import flex.engine.model.DocumentContext
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class LavkaNavigationActionActionTest {
    private val action = LavkaNavigationAction(PATH)
    private val context = mock<DocumentContext>()
    private val actionRouter = mock<ActionRouter>()
    private val actionHandler = LavkaNavigationActionHandler(actionRouter)

    @Test
    fun `Check lavka navigation handler`() {
        actionHandler.handle(action, context)

        verify(actionRouter).openEatsKitWebView(PATH)
    }

    companion object {
        private const val PATH = "path"
    }
}
