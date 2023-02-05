package ru.yandex.market.clean.presentation.feature.smartshopping.coin

import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import io.reactivex.Single
import org.junit.Test
import ru.yandex.market.clean.domain.model.SmartCoin
import ru.yandex.market.clean.presentation.feature.catalog.RootCatalogTargetScreen
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.clean.presentation.navigation.TargetScreen
import ru.yandex.market.presentationSchedulersMock

class SmartCoinInformationPresenterTest {

    private val smartCoin = spy(SmartCoin.testBuilder().build())
    private val smartCoinInformationFormatter = mock<SmartCoinInformationFormatter>()
    private val router = mock<Router>()
    private val view = mock<SmartCoinInformationView>()

    @Suppress("DEPRECATION")
    private val analyticsService = mock<ru.yandex.market.analitycs.AnalyticsService>()
    private val smartCoinInformationUseCases = mock<SmartCoinInformationUseCases> {
        on {
            resolveSmartCoinApplicableGoods(any())
        } doReturn Single.just(RootCatalogTargetScreen() as TargetScreen<*>)
    }
    private val args = SmartCoinSimpleInformationArguments(smartCoin)
    private val schedulers = presentationSchedulersMock()
    private val presenter =
        SmartCoinInformationPresenter(
            schedulers,
            args,
            smartCoinInformationFormatter,
            router,
            analyticsService,
            smartCoinInformationUseCases
        )

    @Test
    fun `Close view after navigate`() {
        whenever(smartCoinInformationFormatter.format(any()))
            .thenReturn(smartCoinInformationVoTestInstance())

        presenter.attachView(view)
        presenter.onBottomActionClick(SmartCoinInformationBottomSelectGoodsAction("text"))

        verify(view).closeSelf()
    }
}