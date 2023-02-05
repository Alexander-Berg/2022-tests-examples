package ru.yandex.market.clean.presentation.feature.trust.vo.popover.formatter

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.market.clean.domain.model.TrustConfiguration
import ru.yandex.market.clean.presentation.feature.trust.vo.popover.TrustOfficialShopVo

class TrustOfficialShopFormatterTest {

    private val formatter = TrustOfficialShopFormatter()

    @Test
    fun `Test format official text empty`() {
        val configuration = TrustConfiguration(official = "", recommended = "")

        val result = formatter.format(configuration)

        assertThat(result.ignoreError()).isNull()
    }

    @Test
    fun `Test default format`() {
        val configuration = TrustConfiguration(official = OFFICIAL_TEXT, recommended = "")

        val expected = TrustOfficialShopVo(subtitle = OFFICIAL_TEXT)

        val actual = formatter.format(configuration)

        assertThat(actual.throwError()).isEqualTo(expected)
    }

    private companion object {
        const val OFFICIAL_TEXT = "Какой то текст, который говорит о том что магазин - оффициальный"
    }
}