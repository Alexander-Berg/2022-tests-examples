package ru.yandex.market.clean.presentation.feature.smartshopping

import org.mockito.kotlin.any
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.market.common.dateformatter.DateFormatter
import ru.yandex.market.clean.domain.model.SmartCoin
import ru.yandex.market.clean.domain.model.smartCoinInformationTestInstance
import ru.yandex.market.common.android.ResourcesManager

class SmartCoinInformationFormatterTest {

    private val dateTimeFormatter = mock<DateFormatter> {
        on { formatShort(any()) } doReturn ""
        on { formatNumericShort(any()) } doReturn ""
    }
    private val resourcesDataStore = mock<ResourcesManager> {
        on { getFormattedString(any(), anyVararg()) } doReturn ""
    }
    private val formatter = SmartCoinInformationFormatter(resourcesDataStore, dateTimeFormatter)

    @Test
    fun `Capitalizes title`() {
        val information = smartCoinInformationTestInstance(title = "title")

        val smartCoin = SmartCoin.testBuilder()
            .information(information)
            .build()

        val formatted = formatter.format(smartCoin, false)

        assertThat(formatted.title).isEqualTo("Title")
    }

    @Test
    fun `Capitalizes description`() {
        val information = smartCoinInformationTestInstance(title = "description")

        val smartCoin = SmartCoin.testBuilder()
            .information(information)
            .build()

        val formatted = formatter.format(smartCoin, false)

        assertThat(formatted.description).isEqualTo("Description")
    }
}