package ru.yandex.market.clean.presentation.feature.profile.promo.helpisnear

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.beru.android.R
import ru.yandex.market.clean.domain.model.HelpIsNearSubscriptionStatus
import ru.yandex.market.clean.domain.model.profile.ProfileMenuItemType
import ru.yandex.market.clean.presentation.feature.profile.menu.HelpIsNearProfileMenuVo
import ru.yandex.market.domain.money.model.Money
import ru.yandex.market.domain.money.model.moneyTestInstance
import ru.yandex.market.common.android.ResourcesManager
import ru.yandex.market.clean.presentation.formatter.MoneyFormatter

class ProfileHelpIsNearFormatterTest {

    private val resourcesDataStore = mock<ResourcesManager> {
        on { getString(R.string.profile_help_is_near_subtitle_subscriber) } doReturn SUBSCRIBED_TITLE
        on { getString(R.string.profile_help_is_near_subtitle_not_subscriber) } doReturn NOT_SUBSCRIBED_TITLE
    }
    private val moneyFormatter = mock<MoneyFormatter>()

    private val formatter = ProfileHelpIsNearFormatter(resourcesDataStore, moneyFormatter)

    @Test
    fun `format subscribed status`() {
        val money = Money.createRub(300)
        val moneyValue = "300 рублей"
        whenever(moneyFormatter.formatPrice(money)) doReturn moneyValue

        val expected = HelpIsNearProfileMenuVo(
            ProfileMenuItemType.HELP_IS_NEAR,
            true,
            SUBSCRIBED_TITLE,
            moneyValue
        )
        val actual = formatter.format(HelpIsNearSubscriptionStatus(true, money))

        assertThat(expected).isEqualTo(actual)
    }

    @Test
    fun `format not subscribed status`() {
        val expected = HelpIsNearProfileMenuVo(
            ProfileMenuItemType.HELP_IS_NEAR,
            false,
            NOT_SUBSCRIBED_TITLE,
            ""
        )
        val actual = formatter.format(HelpIsNearSubscriptionStatus(false, moneyTestInstance()))

        assertThat(expected).isEqualTo(actual)
    }

    companion object {
        private const val SUBSCRIBED_TITLE = "подписан"
        private const val NOT_SUBSCRIBED_TITLE = "не подписан"
    }

}
