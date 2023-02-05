package ru.yandex.market.clean.data.mapper.profitabilityindex

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.market.clean.data.mapper.profitabilityindex.ProfitabilityIndexDtoToDomainMapperTestEntity.CMS_PROFITABILITY_INDEX_RESULT_SUCCESS_1
import ru.yandex.market.clean.data.mapper.profitabilityindex.ProfitabilityIndexDtoToDomainMapperTestEntity.PROFITABILITY_INDEX_DTO_1
import ru.yandex.market.clean.data.mapper.profitabilityindex.ProfitabilityIndexDtoToDomainMapperTestEntity.PROFITABILITY_INDEX_DTO_WITH_NULL_INDEX_VALUE
import ru.yandex.market.clean.data.mapper.profitabilityindex.ProfitabilityIndexDtoToDomainMapperTestEntity.PROFITABILITY_INDEX_DTO_WITH_NULL_YESTERDAY_INDEX_DIFF
import ru.yandex.market.clean.domain.model.cms.CmsProfitabilityIndexResult

class ProfitabilityIndexDtoToDomainMapperTest {

    private val mapper = ProfitabilityIndexDtoToDomainMapper()

    @Test
    fun `check correct mapping`() {
        assertThat(mapper.map(PROFITABILITY_INDEX_DTO_1)).isEqualTo(CMS_PROFITABILITY_INDEX_RESULT_SUCCESS_1)
    }

    @Test
    fun `check indexValue null failure result`() {
        assertThat(mapper.map(PROFITABILITY_INDEX_DTO_WITH_NULL_INDEX_VALUE))
            .isEqualTo(CmsProfitabilityIndexResult.Failure)
    }

    @Test
    fun `check yesterdayIndexDiff null failure result`() {
        assertThat(mapper.map(PROFITABILITY_INDEX_DTO_WITH_NULL_YESTERDAY_INDEX_DIFF))
            .isEqualTo(CmsProfitabilityIndexResult.Failure)
    }
}