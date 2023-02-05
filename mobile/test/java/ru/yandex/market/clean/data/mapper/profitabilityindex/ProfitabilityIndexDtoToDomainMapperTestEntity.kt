package ru.yandex.market.clean.data.mapper.profitabilityindex

import ru.yandex.market.clean.data.model.dto.profitabilityindex.ProfitabilityIndexCategoryImageDto
import ru.yandex.market.clean.data.model.dto.profitabilityindex.ProfitabilityIndexDto
import ru.yandex.market.clean.data.model.dto.profitabilityindex.ProfitabilityIndexIndexStructureDto
import ru.yandex.market.clean.domain.model.cms.CmsProfitabilityIndexResult
import ru.yandex.market.clean.domain.model.cms.CmsProfitabilityIndexResult.Success.Category
import ru.yandex.market.clean.domain.model.cms.CmsProfitabilityIndexResult.Success.IndexStructure

object ProfitabilityIndexDtoToDomainMapperTestEntity {

    private val PROFITABILITY_INDEX_STRUCTURE_DTO_1 = ProfitabilityIndexIndexStructureDto(
        priceValue = 3.9f,
        discountValue = 1f,
        promoValue = 1.57f,
        cashbackValue = 2f
    )

    private val PROFITABILITY_INDEX_STRUCTURE_DTO_2 = ProfitabilityIndexIndexStructureDto(
        priceValue = 1.5f,
        discountValue = 2f,
        promoValue = 1.74f,
        cashbackValue = 2.96f
    )

    private val PROFITABILITY_INDEX_STRUCTURE_DTO_3 = ProfitabilityIndexIndexStructureDto(
        priceValue = 1f,
        discountValue = 2f,
        promoValue = 3f,
        cashbackValue = 3.5f
    )

    private val PROFITABILITY_INDEX_CATEGORY_DTO_1 = ProfitabilityIndexCategoryImageDto(
        image = "test_image_url_1",
        imageHd = "test_image_url_2"
    )

    private val PROFITABILITY_INDEX_CATEGORY_DTO_2 = ProfitabilityIndexCategoryImageDto(
        image = null,
        imageHd = "test_image_url_hd_2"
    )

    val PROFITABILITY_INDEX_DTO_1 = ProfitabilityIndexDto(
        indexValue = 8.2f,
        yesterdayIndexDiff = 0.2f,
        indexStructure = PROFITABILITY_INDEX_STRUCTURE_DTO_1,
        categories = listOf(PROFITABILITY_INDEX_CATEGORY_DTO_1, PROFITABILITY_INDEX_CATEGORY_DTO_2)
    )

    val PROFITABILITY_INDEX_DTO_2 = ProfitabilityIndexDto(
        indexValue = 5.7f,
        yesterdayIndexDiff = -3.2f,
        indexStructure = PROFITABILITY_INDEX_STRUCTURE_DTO_2,
        categories = listOf(PROFITABILITY_INDEX_CATEGORY_DTO_1, PROFITABILITY_INDEX_CATEGORY_DTO_2)
    )

    val PROFITABILITY_INDEX_DTO_3 = ProfitabilityIndexDto(
        indexValue = 1.2f,
        yesterdayIndexDiff = -1.7f,
        indexStructure = PROFITABILITY_INDEX_STRUCTURE_DTO_3,
        categories = listOf(PROFITABILITY_INDEX_CATEGORY_DTO_1, PROFITABILITY_INDEX_CATEGORY_DTO_2)
    )

    val PROFITABILITY_INDEX_DTO_WITH_NULL_INDEX_VALUE = ProfitabilityIndexDto(
        indexValue = null,
        yesterdayIndexDiff = -3.2f,
        indexStructure = PROFITABILITY_INDEX_STRUCTURE_DTO_1,
        categories = listOf(PROFITABILITY_INDEX_CATEGORY_DTO_1, PROFITABILITY_INDEX_CATEGORY_DTO_2)
    )

    val PROFITABILITY_INDEX_DTO_WITH_NULL_YESTERDAY_INDEX_DIFF = ProfitabilityIndexDto(
        indexValue = 1f,
        yesterdayIndexDiff = null,
        indexStructure = PROFITABILITY_INDEX_STRUCTURE_DTO_1,
        categories = listOf(PROFITABILITY_INDEX_CATEGORY_DTO_1, PROFITABILITY_INDEX_CATEGORY_DTO_2)
    )

    val INDEX_STRUCTURE_DOMAIN = IndexStructure(
        priceValue = PROFITABILITY_INDEX_STRUCTURE_DTO_1.priceValue ?: 0f,
        discountValue = PROFITABILITY_INDEX_STRUCTURE_DTO_1.discountValue ?: 0f,
        promoValue = PROFITABILITY_INDEX_STRUCTURE_DTO_1.promoValue ?: 0f,
        cashbackValue = PROFITABILITY_INDEX_STRUCTURE_DTO_1.cashbackValue ?: 0f
    )

    val CATEGORY_1 = Category(
        imageUrl = PROFITABILITY_INDEX_CATEGORY_DTO_1.image.orEmpty(),
        imageUrlHd = PROFITABILITY_INDEX_CATEGORY_DTO_1.imageHd.orEmpty()
    )
    val CATEGORY_2 = Category(
        imageUrl = PROFITABILITY_INDEX_CATEGORY_DTO_2.image.orEmpty(),
        imageUrlHd = PROFITABILITY_INDEX_CATEGORY_DTO_2.imageHd.orEmpty()
    )

    val CMS_PROFITABILITY_INDEX_RESULT_SUCCESS_1 = CmsProfitabilityIndexResult.Success(
        indexValue = PROFITABILITY_INDEX_DTO_1.indexValue ?: 0f,
        yesterdayIndexDiff = PROFITABILITY_INDEX_DTO_1.yesterdayIndexDiff ?: 0f,
        indexStructure = INDEX_STRUCTURE_DOMAIN,
        categories = listOf(CATEGORY_1, CATEGORY_2)
    )

    val CMS_PROFITABILITY_INDEX_RESULT_SUCCESS_2 = CmsProfitabilityIndexResult.Success(
        indexValue = PROFITABILITY_INDEX_DTO_2.indexValue ?: 0f,
        yesterdayIndexDiff = PROFITABILITY_INDEX_DTO_2.yesterdayIndexDiff ?: 0f,
        indexStructure = INDEX_STRUCTURE_DOMAIN,
        categories = listOf(CATEGORY_1, CATEGORY_2)
    )

    val CMS_PROFITABILITY_INDEX_RESULT_SUCCESS_3 = CmsProfitabilityIndexResult.Success(
        indexValue = PROFITABILITY_INDEX_DTO_3.indexValue ?: 0f,
        yesterdayIndexDiff = PROFITABILITY_INDEX_DTO_3.yesterdayIndexDiff ?: 0f,
        indexStructure = INDEX_STRUCTURE_DOMAIN,
        categories = emptyList()
    )
}