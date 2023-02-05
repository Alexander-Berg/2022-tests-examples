package flex.actions

import flex.actions.navigation.action.AlreadyDeliveredGradeNavigationAction
import flex.actions.navigation.handler.AlreadyDeliveredGradeNavigationActionHandler
import flex.core.model.Action
import flex.engine.action.ActionDispatcher
import flex.engine.model.DocumentContext
import org.junit.Test
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class AlreadyDeliveredGradeNavigationActionTest {
    private val onComplete = mock<Action>()
    private val action = AlreadyDeliveredGradeNavigationAction(ORDER_ID, false, onComplete)
    private val context = mock<DocumentContext>()
    private val actionRouter = mock<ActionRouter>()
    private val actionDispatcher = mock<ActionDispatcher>()
    private val actionHandler = AlreadyDeliveredGradeNavigationActionHandler(actionRouter, actionDispatcher)

    @Test
    fun `Check already delivered navigation action handler`() {
        actionHandler.handle(action, context)

        verify(actionRouter).alreadyDelivered(eq(ORDER_ID.toLong()), eq(action.isArchived), argThat { true })
    }

    companion object {
        private const val ORDER_ID = "1234"
    }
}
