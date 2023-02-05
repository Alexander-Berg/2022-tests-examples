package ru.yandex.market.clean.presentation.feature.cms.item.instruction

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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class InstructionFormatterTest {

    private val formatter = InstructionFormatter()

    @Test
    fun `Format with pharma exp enabled and instructionsPointsKeys`() {
        val specs = cmsSpecificationsTestInstance().copy(
            description = DESCRIPTION,
            fullFormattedDescription = FULL_FORMATTED_DESCRIPTION,
            specifications = CMS_SPECIFICATIONS
        )
        val instructionsPointsKeys = listOf("Тип препарата", "Страна бренда")
        val expectedResult = SpecificationsVo(
            DESCRIPTION,
            FULL_FORMATTED_DESCRIPTION,
            SPECIFICATIONS,
            false,
            true
        )
        val formatted =
            formatter.format(specs, true, 2, instructionsPointsKeys)
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
    fun `Format with pharma exp disabled and instructionsPointsKeys`() {
        val specs = cmsSpecificationsTestInstance().copy(
            description = DESCRIPTION,
            fullFormattedDescription = FULL_FORMATTED_DESCRIPTION,
            specifications = CMS_SPECIFICATIONS
        )
        val instructionsPointsKeys = listOf("Тип препарата", "Страна бренда")
        val expectedResult = SpecificationsVo(
            DESCRIPTION,
            FULL_FORMATTED_DESCRIPTION,
            SPECIFICATIONS,
            false,
            false
        )
        val formatted =
            formatter.format(specs, false, 2, instructionsPointsKeys)
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
    fun `Format with pharma exp enabled and instructionsPointsKeys logic`() {
        val specs = cmsSpecificationsTestInstance().copy(
            description = DESCRIPTION,
            fullFormattedDescription = FULL_FORMATTED_DESCRIPTION,
            specifications = CMS_SPECIFICATIONS
        )
        val instructionsPointsKeys = listOf("Тип препарата", "Страна бренда")
        val expectedResult = SpecificationsVo(
            DESCRIPTION,
            FULL_FORMATTED_DESCRIPTION,
            SPECIFICATIONS.take(1),
            false,
            true
        )
        val formatted =
            formatter.format(specs, true, 1, instructionsPointsKeys)
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
    fun `Format with pharma exp enabled and instructionsPointsKeys null`() {
        val specs = cmsSpecificationsTestInstance().copy(
            description = DESCRIPTION,
            fullFormattedDescription = FULL_FORMATTED_DESCRIPTION,
            specifications = CMS_SPECIFICATIONS
        )
        val expectedResult = SpecificationsVo(
            DESCRIPTION,
            FULL_FORMATTED_DESCRIPTION,
            SPECIFICATIONS,
            false,
            true
        )
        val formatted =
            formatter.format(specs, true, null, null)
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
    fun `Format CmsSpecifications to list of ProductCharacteristicsEntryVo`() {
        val specs = cmsSpecificationsTestInstance()
        val expectedResult = listOf(
            ProductCharacteristicsEntryVo(
                name = "name",
                value = "value"
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
    }

}