package ru.yandex.market.clean.presentation.formatter

import android.os.Build
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.market.clean.domain.model.cms.CmsSpecifications
import ru.yandex.market.clean.domain.model.cms.cmsSpecificationsTestInstance
import ru.yandex.market.clean.presentation.feature.cms.item.specs.SpecificationsVo
import ru.yandex.market.clean.presentation.vo.ProductCharacteristicsEntryVo
import ru.yandex.market.clean.presentation.vo.ProductCharacteristicsSectionVo
import ru.yandex.market.domain.specifications.model.productSpecificationsTestInstance

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class SpecificationsFormatterTest {

    private val formatter = SpecificationsFormatter()

    @Test
    fun `Format pharma CmsSpecifications with exp enabled`() {
        val specs = cmsSpecificationsTestInstance().copy(
            description = DESCRIPTION,
            fullFormattedDescription = FULL_FORMATTED_DESCRIPTION,
            pharmaSpecification = CMS_PHARMA_SPECIFICATIONS,
            isPharma = true
        )
        val expectedResult = SpecificationsVo(
            DESCRIPTION,
            FULL_FORMATTED_DESCRIPTION,
            SPECIFICATIONS,
            false,
            true
        )
        val formatted = formatter.format(specs, true)
        val actualResult = formatted.copy(
            description = formatted.description.toString(),
            fullFormattedDescription = formatted.fullFormattedDescription.toString(),
            specifications = formatted.specifications.map {
                it.copy(
                    name = it.name.toString(),
                    value = it.value.toString()
                )
            }
        )
        assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun `Format pharma CmsSpecifications with exp disabled`() {
        val specs = cmsSpecificationsTestInstance().copy(
            description = DESCRIPTION,
            fullFormattedDescription = FULL_FORMATTED_DESCRIPTION,
            specifications = CMS_SPECIFICATIONS,
            isPharma = false
        )
        val expectedResult = SpecificationsVo(
            DESCRIPTION,
            FULL_FORMATTED_DESCRIPTION,
            SPECIFICATIONS,
            false,
            false
        )
        val formatted = formatter.format(specs, false)
        val actualResult = formatted.copy(
            description = formatted.description.toString(),
            fullFormattedDescription = formatted.fullFormattedDescription.toString(),
            specifications = formatted.specifications.map {
                it.copy(
                    name = it.name.toString(),
                    value = it.value.toString()
                )
            }
        )
        assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun `Format ProductSpecifications to list of ProductCharacteristicsSectionVo`() {
        val specs = productSpecificationsTestInstance()
        val expectedResult = listOf(
            ProductCharacteristicsSectionVo(
                name = "name",
                entries = listOf(
                    ProductCharacteristicsEntryVo(
                        name = "name",
                        value = "value"
                    )
                )
            )
        )
        val actualResult = formatter.format(specs)
        assertThat(actualResult).isEqualTo(expectedResult)
    }

    companion object {
        private const val DESCRIPTION = "Description"
        private const val FULL_FORMATTED_DESCRIPTION = "Full Formatted Description"
        private val SPECIFICATIONS = listOf(
            SpecificationsVo.SpecificationVo(
                "Тип препарата",
                "лекарственный препарат"
            ),
            SpecificationsVo.SpecificationVo(
                "Страна бренда",
                "Россия"
            )
        )
        private val CMS_SPECIFICATIONS = listOf(
            CmsSpecifications.Specification(
                "Тип препарата",
                "лекарственный препарат"
            ),
            CmsSpecifications.Specification(
                "Страна бренда",
                "Россия"
            )
        )
        private val CMS_PHARMA_SPECIFICATIONS = listOf(
            CmsSpecifications.PharmaSpecification(
                "Тип препарата",
                "лекарственный препарат"
            ),
            CmsSpecifications.PharmaSpecification(
                "Страна бренда",
                "Россия"
            )
        )
    }
}