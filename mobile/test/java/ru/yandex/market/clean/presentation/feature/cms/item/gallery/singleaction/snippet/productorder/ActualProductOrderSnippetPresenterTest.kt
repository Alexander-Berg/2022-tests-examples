package ru.yandex.market.clean.presentation.feature.cms.item.gallery.singleaction.snippet.productorder

import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.yandex.market.analytics.facades.LavkaAnalytics
import ru.yandex.market.clean.domain.model.EatsKitService
import ru.yandex.market.clean.presentation.feature.cart.vo.ProductOrderTypeVo
import ru.yandex.market.clean.presentation.feature.cms.model.CmsActualProductOrderVo
import ru.yandex.market.clean.presentation.feature.eatskit.EatsKitWebViewArguments
import ru.yandex.market.clean.presentation.feature.eatskit.bottomsheet.EatsKitWebViewBottomSheetTargetScreen
import ru.yandex.market.clean.presentation.formatter.ProductOrderTypeFormatter
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.clean.presentation.navigation.Screen
import ru.yandex.market.presentationSchedulersMock
import ru.yandex.market.utils.EquableCharSequence

class ActualProductOrderSnippetPresenterTest {

    private val viewObject = CmsActualProductOrderVo(
        type = ProductOrderTypeVo.LAVKA,
        orderId = "orderId",
        status = "status",
        statusText = EquableCharSequence("statusText", "statusText"),
        substatusText = EquableCharSequence("subStatusText", "subStatusText"),
        imageUrl = "imageUrl",
        countMoreItems = EquableCharSequence("countMoreItems", "countMoreItems"),
        trackingButtonText = EquableCharSequence("trackingButtonText", "trackingButtonText"),
        trackingUrl = "trackingUrl"
    )
    private val productOrderTypeFormatter = mock<ProductOrderTypeFormatter> {
        on { formatToEatsKitService(any()) } doReturn EatsKitService.LAVKA
    }
    private val router = mock<Router> {
        on { currentScreen } doReturn Screen.HOME
    }
    private val analytics = mock<LavkaAnalytics>()
    private val viewState = mock<`ActualProductOrderView$$State`>()

    private val presenter = ActualProductOrderSnippetPresenter(
        presentationSchedulersMock(),
        viewObject,
        productOrderTypeFormatter,
        router,
        analytics
    )

    @Before
    fun setup() {
        presenter.setViewState(viewState)
    }

    @Test
    fun `Show data on attach`() {
        presenter.attachView(viewState)

        verify(viewState).showData(viewObject)
    }

    @Test
    fun `Navigate to track lavka order`() {
        presenter.onTrackOrderClick()

        val expectedTarget = EatsKitWebViewBottomSheetTargetScreen(
            EatsKitWebViewArguments(viewObject.trackingUrl, EatsKitService.LAVKA)
        )

        verify(router).navigateTo(expectedTarget)
    }

    @Test
    fun `Send lavka snippet visible analytics only on first attach`() {
        presenter.attachView(viewState)
        presenter.attachView(viewState)

        verify(analytics).lavkaActualOrderSnippetVisible(
            viewObject.orderId,
            viewObject.status
        )
    }

    @Test
    fun `Send analytics on track lavka order click`() {
        presenter.onTrackOrderClick()

        verify(analytics).lavkaActualOrderSnippetTrackingClicked(
            viewObject.orderId,
            viewObject.status
        )
    }
}