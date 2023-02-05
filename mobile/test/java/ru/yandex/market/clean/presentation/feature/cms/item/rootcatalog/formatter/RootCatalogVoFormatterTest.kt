package ru.yandex.market.clean.presentation.feature.cms.item.rootcatalog.formatter

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.market.clean.domain.model.cms.garson.RootCatalogGroupGarson
import ru.yandex.market.clean.domain.model.cms.garson.RootCatalogNodeGarson
import ru.yandex.market.clean.domain.model.cms.garson.RootCatalogNodeType
import ru.yandex.market.clean.domain.model.navigationNodeTestInstance
import ru.yandex.market.clean.presentation.feature.cms.item.rootcatalog.item.RootCatalogGroupItem
import ru.yandex.market.clean.presentation.feature.cms.item.rootcatalog.item.RootCatalogNodeItem
import ru.yandex.market.clean.presentation.feature.cms.item.rootcatalog.model.RootCatalogGroupVo
import ru.yandex.market.clean.presentation.feature.cms.item.rootcatalog.model.RootCatalogNodeVo
import ru.yandex.market.domain.media.model.MeasuredImageReference

class RootCatalogVoFormatterTest {

    private val formatter = RootCatalogVoFormatter()

    private val navigationNode = navigationNodeTestInstance(
        childNodes = listOf(
            navigationNodeTestInstance(nid = "1", name = "test_node_name_1"),
            navigationNodeTestInstance(nid = "2", name = "test_node_name_2"),
            navigationNodeTestInstance(nid = "3", name = "test_node_name_3"),
            navigationNodeTestInstance(nid = "4", name = "test_node_name_4"),
            navigationNodeTestInstance(nid = "5", name = "test_node_name_5"),
        )
    )
    private val onItemClickActionMock: (RootCatalogNodeVo) -> Unit = {}
    private val onItemShownActionMock: (RootCatalogNodeVo) -> Unit = {}

    private val categoryGarson = RootCatalogNodeGarson.Category(
        width = 2,
        image = MeasuredImageReference.empty(),
        nid = 1L
    )
    private val specialGarson = RootCatalogNodeGarson.Special(
        width = 2,
        image = MeasuredImageReference.empty(),
        title = "title_1",
        type = RootCatalogNodeType.LAVKA,
        url = "url_1"
    )

    private val inputGarsons = listOf(
        RootCatalogGroupGarson(
            title = "group_garson_title_1",
            nodes = listOf(
                categoryGarson,
                categoryGarson.copy(nid = 2L),
                categoryGarson.copy(nid = 5L),
                specialGarson,
                specialGarson.copy(title = "title_2", type = RootCatalogNodeType.EXPRESS, url = "url_2"),
                specialGarson.copy(title = "title_3", type = RootCatalogNodeType.DISCOUNT, url = "url_3"),
                specialGarson.copy(title = "title_4", type = RootCatalogNodeType.UNKNOWN, url = "url_4"),
            )
        ),
        RootCatalogGroupGarson(
            title = "",
            nodes = listOf(
                categoryGarson.copy(nid = 4L),
                categoryGarson.copy(nid = 6L),
                specialGarson.copy(title = "title_5", type = RootCatalogNodeType.LAVKA, url = "url_5"),
            )
        ),
        RootCatalogGroupGarson(
            title = "group_garson_title_3",
            nodes = listOf()
        )
    )

    private val analyticsData = RootCatalogNodeVo.AnalyticsData(0, "group_garson_title_1")

    private val categoryVo = RootCatalogNodeVo.Category(
        analyticsData = analyticsData,
        title = "test_node_name_1",
        weight = 2,
        image = MeasuredImageReference.empty(),
        navigationNode = navigationNode.childNodes[0]
    )
    private val specialVo = RootCatalogNodeVo.Special(
        analyticsData = analyticsData.copy(),
        title = "title_1",
        weight = 2,
        image = MeasuredImageReference.empty(),
        type = RootCatalogNodeType.LAVKA,
        deeplink = "url_1"
    )

    private val expectedResult = listOf(
        RootCatalogGroupItem(
            vo = RootCatalogGroupVo(
                title = "group_garson_title_1",
                nodes = listOf(
                    RootCatalogNodeItem(
                        vo = categoryVo,
                        onItemClickAction = onItemClickActionMock,
                        onItemShownAction = onItemShownActionMock,
                    ),
                    RootCatalogNodeItem(
                        vo = categoryVo.copy(
                            analyticsData = analyticsData.copy(commonPosition = 1),
                            title = "test_node_name_2",
                            navigationNode = navigationNode.childNodes[1]
                        ),
                        onItemClickAction = onItemClickActionMock,
                        onItemShownAction = onItemShownActionMock,
                    ),
                    RootCatalogNodeItem(
                        vo = categoryVo.copy(
                            analyticsData = analyticsData.copy(commonPosition = 2),
                            title = "test_node_name_5",
                            navigationNode = navigationNode.childNodes[4]
                        ),
                        onItemClickAction = onItemClickActionMock,
                        onItemShownAction = onItemShownActionMock,
                    ),
                    RootCatalogNodeItem(
                        vo = specialVo.copy(analyticsData = analyticsData.copy(commonPosition = 3)),
                        onItemClickAction = onItemClickActionMock,
                        onItemShownAction = onItemShownActionMock,
                    ),
                    RootCatalogNodeItem(
                        vo = specialVo.copy(
                            analyticsData = analyticsData.copy(commonPosition = 4),
                            title = "title_2",
                            type = RootCatalogNodeType.EXPRESS,
                            deeplink = "url_2"
                        ),
                        onItemClickAction = onItemClickActionMock,
                        onItemShownAction = onItemShownActionMock,
                    ),
                    RootCatalogNodeItem(
                        vo = specialVo.copy(
                            analyticsData = analyticsData.copy(commonPosition = 5),
                            title = "title_3",
                            type = RootCatalogNodeType.DISCOUNT,
                            deeplink = "url_3"
                        ),
                        onItemClickAction = onItemClickActionMock,
                        onItemShownAction = onItemShownActionMock,
                    ),
                    RootCatalogNodeItem(
                        vo = specialVo.copy(
                            analyticsData = analyticsData.copy(commonPosition = 6),
                            title = "title_4",
                            type = RootCatalogNodeType.UNKNOWN,
                            deeplink = "url_4"
                        ),
                        onItemClickAction = onItemClickActionMock,
                        onItemShownAction = onItemShownActionMock,
                    )
                )
            ),
            isLastItem = false
        ),
        RootCatalogGroupItem(
            vo = RootCatalogGroupVo(
                title = "",
                nodes = listOf(
                    RootCatalogNodeItem(
                        vo = categoryVo.copy(
                            analyticsData = analyticsData.copy(commonPosition = 7, sectionName = ""),
                            title = "test_node_name_4",
                            navigationNode = navigationNode.childNodes[3]
                        ),
                        onItemClickAction = onItemClickActionMock,
                        onItemShownAction = onItemShownActionMock,
                    ),
                    RootCatalogNodeItem(
                        vo = specialVo.copy(
                            analyticsData = analyticsData.copy(commonPosition = 8, sectionName = ""),
                            title = "title_5",
                            type = RootCatalogNodeType.LAVKA,
                            deeplink = "url_5"
                        ),
                        onItemClickAction = onItemClickActionMock,
                        onItemShownAction = onItemShownActionMock,
                    )
                )
            ),
            isLastItem = true
        )
    )

    private val expectedResultWithoutCategories = listOf(
        RootCatalogGroupItem(
            vo = RootCatalogGroupVo(
                title = "group_garson_title_1",
                nodes = listOf(
                    RootCatalogNodeItem(
                        vo = specialVo.copy(analyticsData = analyticsData.copy(commonPosition = 0)),
                        onItemClickAction = onItemClickActionMock,
                        onItemShownAction = onItemShownActionMock,
                    ),
                    RootCatalogNodeItem(
                        vo = specialVo.copy(
                            analyticsData = analyticsData.copy(commonPosition = 1),
                            title = "title_2",
                            type = RootCatalogNodeType.EXPRESS,
                            deeplink = "url_2"
                        ),
                        onItemClickAction = onItemClickActionMock,
                        onItemShownAction = onItemShownActionMock,
                    ),
                    RootCatalogNodeItem(
                        vo = specialVo.copy(
                            analyticsData = analyticsData.copy(commonPosition = 2),
                            title = "title_3",
                            type = RootCatalogNodeType.DISCOUNT,
                            deeplink = "url_3"
                        ),
                        onItemClickAction = onItemClickActionMock,
                        onItemShownAction = onItemShownActionMock,
                    ),
                    RootCatalogNodeItem(
                        vo = specialVo.copy(
                            analyticsData = analyticsData.copy(commonPosition = 3),
                            title = "title_4",
                            type = RootCatalogNodeType.UNKNOWN,
                            deeplink = "url_4"
                        ),
                        onItemClickAction = onItemClickActionMock,
                        onItemShownAction = onItemShownActionMock,
                    )
                )
            ),
            isLastItem = false
        ),
        RootCatalogGroupItem(
            vo = RootCatalogGroupVo(
                title = "",
                nodes = listOf(
                    RootCatalogNodeItem(
                        vo = specialVo.copy(
                            analyticsData = analyticsData.copy(commonPosition = 4, sectionName = ""),
                            title = "title_5",
                            type = RootCatalogNodeType.LAVKA,
                            deeplink = "url_5"
                        ),
                        onItemClickAction = onItemClickActionMock,
                        onItemShownAction = onItemShownActionMock,
                    )
                )
            ),
            isLastItem = true
        )
    )

    @Test
    fun `check correct mapping`() {
        val result = formatter.format(inputGarsons, navigationNode, onItemClickActionMock, onItemShownActionMock)
        assertThat(result.containsAll(expectedResult)).isTrue
        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun `check the equality of sizes`() {
        val result = formatter.format(inputGarsons, navigationNode, onItemClickActionMock, onItemShownActionMock)
        assertThat(result.size == expectedResult.size).isTrue
    }

    @Test
    fun `check mapping without node`() {
        val result = formatter.format(inputGarsons, null, onItemClickActionMock, onItemShownActionMock)
        assertThat(result).isEqualTo(expectedResultWithoutCategories)
    }
}