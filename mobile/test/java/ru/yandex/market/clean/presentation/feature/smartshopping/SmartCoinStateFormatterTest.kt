package ru.yandex.market.clean.presentation.feature.smartshopping

import org.mockito.kotlin.mock
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import ru.yandex.market.utils.createDate
import ru.yandex.market.datetime.DateTimeProvider
import ru.yandex.market.clean.domain.model.SmartCoin
import ru.yandex.market.extensions.minusDays
import ru.yandex.market.extensions.plusDays

class SmartCoinStateFormatterTest {

    private val now = createDate(2018, 9, 11)
    private val dateTimeProvider = mock<DateTimeProvider> {
        on { currentDateTime } doReturn now
    }

    private val formatter = SmartCoinStateFormatter(dateTimeProvider)

    @Test
    fun `Treats coin as active when expiration date is farther than 7 days`() {
        val coin = SmartCoin.testBuilder()
            .endDate(now plusDays 7)
            .build()

        val formatted = formatter.format(coin)

        assertThat(formatted).isEqualTo(SmartCoinState.ACTIVE)
    }

    @Test
    fun `Treats coin as nearly expired when expiration date is lower than 7 days`() {
        val coin = SmartCoin.testBuilder()
            .endDate(now plusDays 6)
            .build()

        val formatted = formatter.format(coin)

        assertThat(formatted).isEqualTo(SmartCoinState.NEARLY_EXPIRED)
    }

    @Test
    fun `Treats coin as nearly expired when expiration date is today`() {
        val coin = SmartCoin.testBuilder()
            .endDate(now)
            .build()

        val formatted = formatter.format(coin)

        assertThat(formatted).isEqualTo(SmartCoinState.NEARLY_EXPIRED)
    }

    @Test
    fun `Treats coin as expired when expiration date is passed`() {
        val coin = SmartCoin.testBuilder()
            .endDate(now minusDays 2)
            .build()

        val formatted = formatter.format(coin)

        assertThat(formatted).isEqualTo(SmartCoinState.EXPIRED)
    }

    @Test
    fun `Treats coin as unknown when expiration date null`() {
        val coin = SmartCoin.testBuilder()
            .endDate(null)
            .build()

        val formatted = formatter.format(coin)

        assertThat(formatted).isEqualTo(SmartCoinState.UNKNOWN)
    }
}