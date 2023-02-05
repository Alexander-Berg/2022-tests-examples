package ru.yandex.market.clean.presentation.feature.trust.vo.popover.formatter

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.yandex.market.clean.domain.model.organizationTestInstance
import ru.yandex.market.clean.domain.model.supplierTestInstance
import ru.yandex.market.clean.presentation.feature.trust.vo.popover.TrustLegalShopInfoVo
import ru.yandex.market.common.android.ResourcesManager

class TrustLegalShopInfoFormatterTest {

    private val resourceManager = mock<ResourcesManager> {
        on { getFormattedString(any(), any()) } doReturn RESOURCE_STRING
    }

    private val formatter = TrustLegalShopInfoFormatter(resourceManager)

    @Test
    fun `Test format name empty`() {
        val supplier = supplierTestInstance(name = "")

        val result = formatter.format(supplier)

        assertThat(result.ignoreError()).isNull()
    }

    @Test
    fun `Test format all info empty`() {
        val supplier = supplierTestInstance(
            organizations = listOf(
                organizationTestInstance(
                    name = "",
                    contactPhone = "",
                    ogrn = "",
                    address = ""
                )
            )
        )

        val result = formatter.format(supplier)

        assertThat(result.ignoreError()).isNull()
    }

    @Test
    fun `Test default format`() {
        val supplier = supplierTestInstance()

        val expected = TrustLegalShopInfoVo(
            legalAddress = RESOURCE_STRING,
            ogrn = RESOURCE_STRING,
            owner = RESOURCE_STRING,
            shopName = supplier.name,
            inn = null,
        )

        val actual = formatter.format(supplier).throwError()

        assertThat(actual).isEqualTo(expected)
    }

    private companion object {
        const val RESOURCE_STRING = "Просто строка"
    }
}
