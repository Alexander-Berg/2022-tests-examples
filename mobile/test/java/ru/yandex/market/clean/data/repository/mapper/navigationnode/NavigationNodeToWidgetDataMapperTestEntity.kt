package ru.yandex.market.clean.data.repository.mapper.navigationnode

import ru.yandex.market.clean.domain.model.NavigationNodeImages
import ru.yandex.market.clean.domain.model.NavigationNodeImagesItem
import ru.yandex.market.clean.domain.model.NodeImagesItem
import ru.yandex.market.clean.domain.model.cms.CmsNavigationNode
import ru.yandex.market.clean.domain.model.cms.WidgetData
import ru.yandex.market.clean.domain.model.navigationNodeTestInstance
import ru.yandex.market.domain.media.model.measuredImageReferenceTestInstance

object NavigationNodeToWidgetDataMapperTestEntity {

    private const val CHILD_NAVIGATION_NODE_ID_1 = 1L
    private const val CHILD_NAVIGATION_NODE_ID_2 = 2L

    private val NODE_IMAGE_ITEM = NodeImagesItem(null, 0, 0, "imageUrl2")

    val INPUT_IMAGES = NavigationNodeImages(
        listOf(
            NavigationNodeImagesItem(CHILD_NAVIGATION_NODE_ID_2, NODE_IMAGE_ITEM, false),
            NavigationNodeImagesItem(3L, NODE_IMAGE_ITEM.copy(url = "imageUrl3"), false)
        )
    )

    private val INPUT_CHILD_NODE_1 = navigationNodeTestInstance(
        id = CHILD_NAVIGATION_NODE_ID_1.toString(),
        childNodes = emptyList(),
        image = measuredImageReferenceTestInstance(url = "testChildUrl1")
    )

    private val INPUT_CHILD_NODE_2 = navigationNodeTestInstance(
        id = CHILD_NAVIGATION_NODE_ID_2.toString(),
        childNodes = emptyList(),
        image = measuredImageReferenceTestInstance(url = "testChildUrl2")
    )

    val INPUT_ROOT_NODE = navigationNodeTestInstance(
        id = "0",
        name = "INPUT_NAVIGATION_NODE",
        childNodes = listOf(INPUT_CHILD_NODE_1, INPUT_CHILD_NODE_2)
    )

    private val EXPECTED_CHILD_NODE_1 = INPUT_CHILD_NODE_1

    private val EXPECTED_CHILD_NODE_2 = INPUT_CHILD_NODE_2.copy(
        image = measuredImageReferenceTestInstance(url = NODE_IMAGE_ITEM.url!!)
    )

    val EXPECTED_WIDGET_DATA = WidgetData(
        tabs = emptyList(),
        cmsItems = listOf(
            CmsNavigationNode(INPUT_ROOT_NODE, true),
            CmsNavigationNode(EXPECTED_CHILD_NODE_1, false),
            CmsNavigationNode(EXPECTED_CHILD_NODE_2, false)
        ),
        title = INPUT_ROOT_NODE.name,
        linkParams = null,
        recommendationParams = null,
    )
}
