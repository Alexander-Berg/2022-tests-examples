package ru.yandex.market.clean.presentation.feature.cms.item.profitabilityindex.formatter

import ru.beru.android.R
import ru.yandex.market.clean.data.mapper.profitabilityindex.ProfitabilityIndexDtoToDomainMapperTestEntity.CATEGORY_1
import ru.yandex.market.clean.data.mapper.profitabilityindex.ProfitabilityIndexDtoToDomainMapperTestEntity.CATEGORY_2
import ru.yandex.market.clean.data.mapper.profitabilityindex.ProfitabilityIndexDtoToDomainMapperTestEntity.CMS_PROFITABILITY_INDEX_RESULT_SUCCESS_1
import ru.yandex.market.clean.data.mapper.profitabilityindex.ProfitabilityIndexDtoToDomainMapperTestEntity.CMS_PROFITABILITY_INDEX_RESULT_SUCCESS_2
import ru.yandex.market.clean.data.mapper.profitabilityindex.ProfitabilityIndexDtoToDomainMapperTestEntity.CMS_PROFITABILITY_INDEX_RESULT_SUCCESS_3
import ru.yandex.market.clean.data.mapper.profitabilityindex.ProfitabilityIndexDtoToDomainMapperTestEntity.INDEX_STRUCTURE_DOMAIN
import ru.yandex.market.clean.presentation.feature.cms.model.ProfitabilityIndexVo
import ru.yandex.market.uikit.circulargraph.CircularGraphItem
import java.math.RoundingMode

object ProfitabilityIndexVoFormatterTestEntity {

    private val GRAPH_ITEMS_1 = CircularGraphItem(INDEX_STRUCTURE_DOMAIN.priceValue.leadToRatio(), R.color.malachite)
    private val GRAPH_ITEMS_2 = CircularGraphItem(INDEX_STRUCTURE_DOMAIN.discountValue.leadToRatio(), R.color.garnet)
    private val GRAPH_ITEMS_3 = CircularGraphItem(INDEX_STRUCTURE_DOMAIN.promoValue.leadToRatio(), R.color.thrush_eggs)
    private val GRAPH_ITEMS_4 = CircularGraphItem(
        INDEX_STRUCTURE_DOMAIN.cashbackValue.leadToRatio(),
        R.color.azure_blue
    )

    val PROFITABILITY_INDEX_VO_1 = ProfitabilityIndexVo(
        graphItems = listOf(GRAPH_ITEMS_1, GRAPH_ITEMS_2, GRAPH_ITEMS_3, GRAPH_ITEMS_4),
        currentValue = CMS_PROFITABILITY_INDEX_RESULT_SUCCESS_1.indexValue.toString(),
        yesterdayIndex = "+${CMS_PROFITABILITY_INDEX_RESULT_SUCCESS_1.yesterdayIndexDiff}",
        categoryThumbnailUrls = listOf(CATEGORY_1.imageUrl, CATEGORY_2.imageUrl)
    )

    val PROFITABILITY_INDEX_VO_2 = ProfitabilityIndexVo(
        graphItems = listOf(GRAPH_ITEMS_1, GRAPH_ITEMS_2, GRAPH_ITEMS_3, GRAPH_ITEMS_4),
        currentValue = CMS_PROFITABILITY_INDEX_RESULT_SUCCESS_2.indexValue.toString(),
        yesterdayIndex = CMS_PROFITABILITY_INDEX_RESULT_SUCCESS_2.yesterdayIndexDiff.toString(),
        categoryThumbnailUrls = listOf(CATEGORY_1.imageUrl, CATEGORY_2.imageUrl)
    )

    val PROFITABILITY_INDEX_VO_3 = ProfitabilityIndexVo(
        graphItems = listOf(GRAPH_ITEMS_1, GRAPH_ITEMS_2, GRAPH_ITEMS_3, GRAPH_ITEMS_4),
        currentValue = CMS_PROFITABILITY_INDEX_RESULT_SUCCESS_3.indexValue.toString(),
        yesterdayIndex = CMS_PROFITABILITY_INDEX_RESULT_SUCCESS_3.yesterdayIndexDiff.toString(),
        categoryThumbnailUrls = emptyList()
    )

    private fun Float.leadToRatio() = toBigDecimal().divide(10f.toBigDecimal(), 2, RoundingMode.HALF_EVEN).toFloat()
}