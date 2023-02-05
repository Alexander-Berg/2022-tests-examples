package flex.actions

import flex.actions.navigation.action.BarcodeNavigationAction
import flex.actions.navigation.handler.BarcodeNavigationActionHandler
import flex.engine.model.DocumentContext
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class BarcodeNavigationActionTest {
    private val barcode = "777"
    private val barcodeData = "888"
    private val action = BarcodeNavigationAction(ORDER_ID, BarcodeNavigationAction.Barcode(barcode, barcodeData))
    private val context = mock<DocumentContext>()
    private val actionRouter = mock<ActionRouter>()
    private val actionHandler = BarcodeNavigationActionHandler(actionRouter)

    @Test
    fun `Check open barcode screen action handler`() {
        actionHandler.handle(action, context)

        verify(actionRouter).openBarcode(ORDER_ID.toLong(), barcode, barcodeData)
    }

    companion object {
        private const val ORDER_ID = "1234"
    }
}
