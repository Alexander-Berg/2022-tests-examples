package ru.yandex.market.clean.presentation.feature.smartshopping.coin

import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import org.hamcrest.Matchers
import org.hamcrest.Matchers.`is`
import org.junit.Assert
import org.junit.Test
import ru.yandex.market.common.dateformatter.DateFormatter
import ru.yandex.market.clean.domain.model.SmartCoin
import ru.yandex.market.clean.domain.model.SmartCoinBonusLink
import ru.yandex.market.clean.presentation.feature.smartshopping.SmartCoinState
import ru.yandex.market.clean.presentation.feature.smartshopping.SmartCoinStateFormatter
import ru.yandex.market.common.android.ResourcesManager

class SmartCoinInformationFormatterTest {

    private val smartCoin = spy(SmartCoin.testBuilder().build())
    private val smartCoinStateFormatter = mock<SmartCoinStateFormatter>()
    private val resourcesDataStore = mock<ResourcesManager> {
        on { getString(any()) } doReturn ""
        on { getFormattedString(any(), any()) } doReturn ""
    }
    private val dateTimeFormatter = mock<DateFormatter> {
        on { formatShort(any()) } doReturn ""
        on { formatNumericShort(any()) } doReturn ""
    }

    private val smartCoinInformationFormatter = SmartCoinInformationFormatter(
        resourcesDataStore,
        dateTimeFormatter,
        smartCoinStateFormatter
    )

    @Test
    fun `Format active smart coin`() {
        whenever(smartCoinStateFormatter.format(any())).thenReturn(SmartCoinState.ACTIVE)

        val formatted = smartCoinInformationFormatter.format(smartCoin)
        Assert.assertThat(formatted.isImageActive, Matchers.equalTo(true))
        Assert.assertThat(formatted.isTextActive, Matchers.equalTo(true))
    }

    @Test
    fun `Format inactive smart coin with orders`() {
        val orderIds = listOf(54321L, 44524L)

        whenever(smartCoinStateFormatter.format(any())).thenReturn(SmartCoinState.INACTIVE)
        whenever(smartCoin.reasonType()).thenReturn(SmartCoin.ReasonType.ORDER)
        whenever(smartCoin.state()).thenReturn(SmartCoin.State.INACTIVE)
        whenever(smartCoin.reasonOrderIds()).thenReturn(orderIds.map { it.toString() })

        val formatted = smartCoinInformationFormatter.format(smartCoin)
        Assert.assertThat(formatted.waitingOrderIds, Matchers.equalTo(orderIds))
    }

    @Test
    fun `Format inactive smart coin without order`() {

        whenever(smartCoinStateFormatter.format(any())).thenReturn(SmartCoinState.INACTIVE)
        whenever(smartCoin.reasonType()).thenReturn(SmartCoin.ReasonType.UNKNOWN)

        val formatted = smartCoinInformationFormatter.format(smartCoin)
        Assert.assertThat(formatted.waitingOrderIds.size, `is`(0))
    }

    @Test
    fun `Format inactive smart coin with bonus link`() {
        val bonusLink = SmartCoinBonusLink("url", "title")
        val action = SmartCoinInformationBottomOpenLinkAction(bonusLink.title, bonusLink.url)

        whenever(smartCoinStateFormatter.format(any())).thenReturn(SmartCoinState.INACTIVE)
        whenever(smartCoin.bonusLink()).thenReturn(bonusLink)

        val formatted = smartCoinInformationFormatter.format(smartCoin)

        Assert.assertEquals(formatted.bottomButtonAction, action)
    }
}