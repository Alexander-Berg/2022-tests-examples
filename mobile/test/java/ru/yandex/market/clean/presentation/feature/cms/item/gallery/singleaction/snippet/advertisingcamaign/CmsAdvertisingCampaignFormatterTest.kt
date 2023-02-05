package ru.yandex.market.clean.presentation.feature.cms.item.gallery.singleaction.snippet.advertisingcamaign

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.beru.android.R
import ru.yandex.market.clean.domain.model.cms.CmsAdvertisingCampaignItem
import ru.yandex.market.domain.media.model.MeasuredImageReference
import ru.yandex.market.domain.media.model.measuredImageReferenceTestInstance
import ru.yandex.market.common.android.ResourcesManager
import ru.yandex.market.utils.Characters
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

@RunWith(Parameterized::class)
class CmsAdvertisingCampaignFormatterTest(
    private val input: CmsAdvertisingCampaignItem,
    private val expectedOutput: CmsAdvertisingCampaignVo
) {
    private val amountFormat = DecimalFormat(
        "###,###",
        DecimalFormatSymbols().apply {
            groupingSeparator = Characters.NON_BREAKING_SPACE
        }
    )

    private val cashbackAmount = amountFormat.format(input.welcomeCashback)
    private val orderThreshold = amountFormat.format(input.orderThreshold)

    private val resourcesDataStore = mock<ResourcesManager> {
        on {
            getFormattedQuantityString(
                R.plurals.cms_widget_advertising_title,
                input.welcomeCashback.toInt(),
                cashbackAmount
            )
        } doReturn TITLE + cashbackAmount
        on {
            getFormattedString(
                R.string.cms_widget_advertising_subtitle,
                orderThreshold
            )
        } doReturn SUBTITLE + orderThreshold
        on { getString(R.string.cms_widget_advertising_close) } doReturn AUTH_PRIMARY_ACTION
        on { getString(R.string.cms_widget_advertising_login) } doReturn NOT_AUTH_PRIMARY_ACTION
        on { getString(R.string.cms_widget_advertising_about_plus) } doReturn AUTH_SECONDARY_ACTION
    }
    private val formatter = CmsAdvertisingCampaignFormatter(resourcesDataStore)

    @Test
    fun format() {
        val formatted = formatter.format(input)
        assertThat(expectedOutput).isEqualTo(formatted)
    }

    companion object {

        private const val AUTH_PRIMARY_ACTION = "здорово"
        private const val NOT_AUTH_PRIMARY_ACTION = "войти"
        private const val AUTH_SECONDARY_ACTION = "про плюс"
        private const val TITLE = "Дарим баллы - "
        private const val SUBTITLE = "За первый заказ в приложении от "

        @Parameterized.Parameters(name = "{index}: {0} -> {1}")
        @JvmStatic
        fun data(): Iterable<Array<*>> = listOf(
            //0
            arrayOf(
                CmsAdvertisingCampaignItem(
                    welcomeCashback = BigDecimal(500),
                    orderThreshold = BigDecimal(3500),
                    isAuthorized = true,
                    icon = measuredImageReferenceTestInstance()
                ),
                CmsAdvertisingCampaignVo(
                    title = TITLE + "500",
                    subtitle = SUBTITLE + "3${Characters.NON_BREAKING_SPACE}500",
                    primaryAction = AdvertisingCampaignButtonAction(
                        title = AUTH_PRIMARY_ACTION,
                        AdvertisingCampaignButtonAction.Action.CLOSE
                    ),
                    secondaryAction = AdvertisingCampaignButtonAction(
                        title = AUTH_SECONDARY_ACTION,
                        AdvertisingCampaignButtonAction.Action.ABOUT_PLUS
                    ),
                    image = measuredImageReferenceTestInstance()
                )
            ),

            //1
            arrayOf(
                CmsAdvertisingCampaignItem(
                    welcomeCashback = BigDecimal(5000),
                    orderThreshold = BigDecimal(35000),
                    isAuthorized = false,
                    icon = MeasuredImageReference.empty()
                ),
                CmsAdvertisingCampaignVo(
                    title = TITLE + "5${Characters.NON_BREAKING_SPACE}000",
                    subtitle = SUBTITLE + "35${Characters.NON_BREAKING_SPACE}000",
                    primaryAction = AdvertisingCampaignButtonAction(
                        title = NOT_AUTH_PRIMARY_ACTION,
                        AdvertisingCampaignButtonAction.Action.LOGIN
                    ),
                    secondaryAction = null,
                    image = MeasuredImageReference.empty()
                )
            ),
        )
    }
}