package ru.yandex.market.clean.presentation.feature.cms.item.profitabilityindex.formatter

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.market.clean.data.mapper.profitabilityindex.ProfitabilityIndexDtoToDomainMapperTestEntity.CMS_PROFITABILITY_INDEX_RESULT_SUCCESS_1
import ru.yandex.market.clean.data.mapper.profitabilityindex.ProfitabilityIndexDtoToDomainMapperTestEntity.CMS_PROFITABILITY_INDEX_RESULT_SUCCESS_2
import ru.yandex.market.clean.data.mapper.profitabilityindex.ProfitabilityIndexDtoToDomainMapperTestEntity.CMS_PROFITABILITY_INDEX_RESULT_SUCCESS_3
import ru.yandex.market.clean.domain.model.cms.CmsProfitabilityIndexResult
import ru.yandex.market.clean.presentation.feature.cms.item.formatter.ProfitabilityIndexStructureVoFormatter
import ru.yandex.market.clean.presentation.feature.cms.item.profitabilityindex.formatter.ProfitabilityIndexVoFormatterTestEntity.PROFITABILITY_INDEX_VO_1
import ru.yandex.market.clean.presentation.feature.cms.item.profitabilityindex.formatter.ProfitabilityIndexVoFormatterTestEntity.PROFITABILITY_INDEX_VO_2
import ru.yandex.market.clean.presentation.feature.cms.item.profitabilityindex.formatter.ProfitabilityIndexVoFormatterTestEntity.PROFITABILITY_INDEX_VO_3
import ru.yandex.market.clean.presentation.feature.cms.model.ProfitabilityIndexVo

@RunWith(Parameterized::class)
class ProfitabilityIndexVoFormatterTest(
    private val domain: CmsProfitabilityIndexResult.Success,
    private val vo: ProfitabilityIndexVo
) {

    private val mapper = ProfitabilityIndexVoFormatter(ProfitabilityIndexStructureVoFormatter())

    @Test
    fun `checking formatting`() {
        assertThat(mapper.format(domain)).isEqualTo(vo)
    }

    companion object {

        @Parameterized.Parameters(name = "{index}: {0} - it should format into {1}")
        @JvmStatic
        fun parameters() = listOf(
            arrayOf(CMS_PROFITABILITY_INDEX_RESULT_SUCCESS_1, PROFITABILITY_INDEX_VO_1),
            arrayOf(CMS_PROFITABILITY_INDEX_RESULT_SUCCESS_2, PROFITABILITY_INDEX_VO_2),
            arrayOf(CMS_PROFITABILITY_INDEX_RESULT_SUCCESS_3, PROFITABILITY_INDEX_VO_3)
        )
    }
}