package ru.yandex.market.ui.view.mvp.cartcounterbutton.adult

import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.yandex.market.activity.model.adult.SkuAdultDisclaimerArguments
import ru.yandex.market.activity.model.adult.SkuAdultDisclaimerTargetScreen
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.domain.adult.model.AdultState
import ru.yandex.market.presentationSchedulersMock

class CartCounterAdultDelegateTest {
    private val useCases = mock<CartCounterAdultUseCases> {
        on { getAdultState() } doReturn Single.just(AdultState.DISABLED)
    }
    private val router = mock<Router>()
    private val schedulers = presentationSchedulersMock()

    private val delegate = CartCounterAdultDelegate(
        useCases = useCases,
        router = router,
        schedulers = schedulers,
    )

    @Test
    fun `Navigate to adult screen if adult is disabled`() {
        val targetScreen = SkuAdultDisclaimerTargetScreen(
            SkuAdultDisclaimerArguments(
                hid = null,
                nid = null,
                skuId = null,
                skuType = null,
                offerId = "1",
                modelId = null
            )
        )
        val onSuccess = {}
        delegate.checkAdult(onSuccess) { targetScreen }
        verify(router).navigateForResult(eq(targetScreen), any())
    }
}