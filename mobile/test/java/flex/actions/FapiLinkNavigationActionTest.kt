package flex.actions

import flex.actions.navigation.action.FapiLinkNavigationAction
import flex.actions.navigation.handler.FapiLinkNavigationActionHandler
import flex.engine.model.DocumentContext
import org.junit.Test
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class FapiLinkNavigationActionTest {

    private val context = mock<DocumentContext>()
    private val actionRouter = mock<ActionRouter>()
    private val actionHandler = FapiLinkNavigationActionHandler(actionRouter)

    @Test
    fun `Check already delivered navigation action handler with lavka link type`() {
        val type = "lavkaLink"
        val link = "link"
        val action = FapiLinkNavigationAction(type, link)
        actionHandler.handle(action, context)

        verify(actionRouter).openEatsKitFlow(link)
    }

    @Test
    fun `Check fapi link navigation action handler`() {
        val type = "deeplink"
        val link = "link"
        val action = FapiLinkNavigationAction(type, link)
        actionHandler.handle(action, context)

        verify(actionRouter).navigateToLink(eq(link), argThat { true })
    }

}
