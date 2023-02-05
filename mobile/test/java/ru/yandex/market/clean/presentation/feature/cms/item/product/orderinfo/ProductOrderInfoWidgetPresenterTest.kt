package ru.yandex.market.clean.presentation.feature.cms.item.product.orderinfo

import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.yandex.market.analytics.offer.OfferAnalyticsFacade
import ru.yandex.market.clean.presentation.feature.sku.AnalyticsParam
import ru.yandex.market.clean.presentation.feature.sku.DeliveryInformationVo

class ProductOrderInfoWidgetPresenterTest {

    private val offerAnalyticsFacade = mock<OfferAnalyticsFacade>()

    val presenter = ProductOrderInfoWidgetPresenter(
        mock(),
        mock(),
        mock(),
        mock(),
        mock(),
        mock(),
        mock(),
        offerAnalyticsFacade,
        mock(),
        mock(),
        mock(),
        mock()
    )

    @Test
    fun `Send Main Delivery Options Visible Event`() {
        val params = mock<AnalyticsParam>()
        val vo = mock<DeliveryInformationVo>() {
            on { analyticsParam } doReturn params
        }
        presenter.sendAnalytics(vo)
        verify(offerAnalyticsFacade).sendMainDeliveryOptionsVisibleEvent(params)
    }
}
