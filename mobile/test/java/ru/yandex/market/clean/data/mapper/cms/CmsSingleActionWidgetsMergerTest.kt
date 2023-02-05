package ru.yandex.market.clean.data.mapper.cms

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.market.clean.domain.model.cms.CmsFont
import ru.yandex.market.clean.domain.model.cms.CmsWidget
import ru.yandex.market.clean.domain.model.cms.CmsWidgetSubtitle
import ru.yandex.market.clean.domain.model.cms.CmsWidgetTitle
import ru.yandex.market.clean.domain.model.cms.CmsWidgetType
import ru.yandex.market.clean.domain.model.cms.GarsonType
import ru.yandex.market.clean.domain.model.cms.garson.MergedWidgetParams
import ru.yandex.market.clean.domain.model.cms.garson.MergedWidgetParamsGarson
import ru.yandex.market.clean.domain.model.cms.garson.actualLavkaOrdersGarsonTestInstance
import ru.yandex.market.clean.domain.model.cms.garson.actualOrdersGarsonTestInstance
import ru.yandex.market.clean.domain.model.cms.garson.plusBenefitsGarsonTestInstance
import ru.yandex.market.clean.domain.model.cms.garson.softUpdateGarsonTestInstance

class CmsSingleActionWidgetsMergerTest {

    private val cmsSingleActionWidgetsMerger = CmsSingleActionWidgetsMerger()

    @Test
    fun `Merge widgets to single widget`() {
        val mergedWidgetGarsons = WIDGETS_SHOULD_BE_MERGED.map { it.garsons() }.flatten().distinct().toMutableList()
        val paramsMapForGarsonMap: MutableMap<GarsonType, MergedWidgetParams> = mutableMapOf()
        WIDGETS_SHOULD_BE_MERGED.forEach { widget ->
            widget.garsons().forEach {
                paramsMapForGarsonMap[it.type()] = MergedWidgetParams(
                    widgetTitle = widget.title(),
                    widgetSubtitle = widget.subtitle(),
                    isReloadable = widget.isReloadable
                )
            }
        }
        mergedWidgetGarsons.add(MergedWidgetParamsGarson(paramsMapForGarsonMap))
        val expectedWidget = CmsWidget.testBuilder()
            .type(CmsWidgetType.SINGLE_ACTION_GALLERY)
            .title(WIDGETS_SHOULD_BE_MERGED.first().title())
            .subtitle(WIDGETS_SHOULD_BE_MERGED.first().subtitle())
            .garsons(mergedWidgetGarsons)
            .build()

        assertThat(cmsSingleActionWidgetsMerger.joinOrderAndSingleActionWidgets(WIDGETS_SHOULD_BE_MERGED))
            .isEqualTo(listOf(expectedWidget))

        assertThat(
            cmsSingleActionWidgetsMerger.joinOrderAndSingleActionWidgets(WIDGETS_SHOULD_BE_MERGED),
        ).isEqualTo(listOf(expectedWidget))
    }

    @Test
    fun `Not modify widgets list if it not contain target widgets`() {
        assertThat(cmsSingleActionWidgetsMerger.joinOrderAndSingleActionWidgets(WIDGETS_SHOULD_NOT_BE_MERGED))
            .isEqualTo(WIDGETS_SHOULD_NOT_BE_MERGED)

        assertThat(
            cmsSingleActionWidgetsMerger.joinOrderAndSingleActionWidgets(WIDGETS_SHOULD_NOT_BE_MERGED)
        ).isEqualTo(WIDGETS_SHOULD_NOT_BE_MERGED)
    }

    companion object {
        private val WIDGETS_SHOULD_BE_MERGED = listOf(
            CmsWidget.testBuilder()
                .type(CmsWidgetType.SINGLE_ACTION)
                .title(CmsWidgetTitle.testBuilder().name("plusBenefitTitle").build())
                .subtitle(CmsWidgetSubtitle("plusBenefitSubTitle", CmsFont.normalCmsFont(), null))
                .garsons(listOf(plusBenefitsGarsonTestInstance()))
                .isReloadable(false)
                .build(),
            CmsWidget.testBuilder()
                .type(CmsWidgetType.SINGLE_ACTION)
                .garsons(listOf(softUpdateGarsonTestInstance()))
                .title(CmsWidgetTitle.testBuilder().name("softUpdateTitle").build())
                .subtitle(CmsWidgetSubtitle("softUpdatesSubTitle", CmsFont.normalCmsFont(), null))
                .isReloadable(false)
                .build(),
            CmsWidget.testBuilder()
                .type(CmsWidgetType.SCROLLBOX)
                .garsons(
                    listOf(
                        actualOrdersGarsonTestInstance(),
                        actualLavkaOrdersGarsonTestInstance()
                    )
                )
                .title(CmsWidgetTitle.testBuilder().name("ordersTitle").build())
                .subtitle(CmsWidgetSubtitle("ordersSubTitle", CmsFont.normalCmsFont(), null))
                .isReloadable(true)
                .build(),
        )

        private val WIDGETS_SHOULD_NOT_BE_MERGED = listOf(
            CmsWidget.testBuilder()
                .type(CmsWidgetType.SCROLLBOX)
                .garsons(listOf(plusBenefitsGarsonTestInstance()))
                .build(),
            CmsWidget.testBuilder()
                .type(CmsWidgetType.MEDIA_CAROUSEL)
                .build(),
            CmsWidget.testBuilder()
                .type(CmsWidgetType.MEDIA_GALLERY)
                .build()
        )
    }
}