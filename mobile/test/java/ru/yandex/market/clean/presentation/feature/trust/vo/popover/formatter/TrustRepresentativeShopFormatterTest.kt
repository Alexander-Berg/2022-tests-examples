package ru.yandex.market.clean.presentation.feature.trust.vo.popover.formatter

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.yandex.market.clean.domain.model.TrustConfiguration
import ru.yandex.market.clean.presentation.feature.trust.vo.popover.TrustRepresentativeShopVo
import ru.yandex.market.common.android.ResourcesManager

class TrustRepresentativeShopFormatterTest {

    private val resourcesManager = mock<ResourcesManager> {
        on { getFormattedString(any(), any()) } doReturn RESOURCES_STRING
    }

    private val formatter = TrustRepresentativeShopFormatter(resourcesManager)

    @Test
    fun `Test format vendor name empty`() {
        val configuration = TrustConfiguration(official = "", recommended = REPRESENTATIVE_TEXT)

        val result = formatter.format(configuration, "")

        assertThat(result.ignoreError()).isNull()
    }


    @Test
    fun `Test format representative text empty`() {
        val configuration = TrustConfiguration(official = "", recommended = "")

        val result = formatter.format(configuration, VENDOR_NAME)

        assertThat(result.ignoreError()).isNull()
    }

    @Test
    fun `Test default format`() {
        val configuration = TrustConfiguration(official = "", recommended = REPRESENTATIVE_TEXT)

        val expected = TrustRepresentativeShopVo(RESOURCES_STRING, REPRESENTATIVE_TEXT)

        val actual = formatter.format(configuration, VENDOR_NAME)

        assertThat(actual.throwError()).isEqualTo(expected)
    }

    private companion object {
        const val RESOURCES_STRING = "Просто строка"
        const val REPRESENTATIVE_TEXT = "Просто текст"
        const val VENDOR_NAME = "Просто вендор"
    }
}