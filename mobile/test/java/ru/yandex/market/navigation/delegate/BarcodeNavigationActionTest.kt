package ru.yandex.market.navigation.delegate

import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.yandex.market.clean.presentation.feature.barcode.BarcodeArguments
import ru.yandex.market.clean.presentation.feature.barcode.BarcodeTargetScreen
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.clean.presentation.navigation.RouterFactory

class BarcodeNavigationActionTest {

    private val router = mock<Router>()

    private val routerFactory = mock<RouterFactory> {
        on { create(any()) } doReturn router
    }

    private val actionRouter = ActionRouterGenerator().generate(routerFactory)

    @Test
    fun `Check open barcode screen`() {
        val barcode = "777"
        val barcodeData = "888"
        actionRouter.openBarcode(
            orderId = ORDER_ID,
            barcode = barcode,
            barcodeData = barcodeData
        )

        val screen = BarcodeTargetScreen(
            BarcodeArguments(
                orderId = ORDER_ID.toString(),
                code = barcode,
                barcodeData = barcodeData,
                isAdult = null
            )
        )
        verify(router).navigateTo(screen)
    }

    companion object {
        private const val ORDER_ID = 1234L
    }
}
