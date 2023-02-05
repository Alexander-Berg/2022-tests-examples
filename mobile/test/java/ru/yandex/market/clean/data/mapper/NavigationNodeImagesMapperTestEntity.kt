package ru.yandex.market.clean.data.mapper

import ru.yandex.market.clean.data.model.dto.cms.CmsNavigationNodeImagesDto
import ru.yandex.market.clean.data.model.dto.cms.NavigationNodeImagesDto
import ru.yandex.market.clean.data.model.dto.cms.NodeImageDto
import ru.yandex.market.clean.domain.model.NavigationNodeImages
import ru.yandex.market.clean.domain.model.NavigationNodeImagesItem
import ru.yandex.market.clean.domain.model.NodeImagesItem

object NavigationNodeImagesMapperTestEntity {

    val NODE_IMAGES_DTO_1 = CmsNavigationNodeImagesDto(
        id = 0,
        images = listOf(
            NavigationNodeImagesDto(
                nodeId = 1,
                imageDto = NodeImageDto("entity1", 200, 100, "url1"),
                isRestrictedAge18 = true
            )
        )
    )

    val EXPECTED_NODE_IMAGES_EXCEPTIONAL_1 = NavigationNodeImages(
        listOf(
            NavigationNodeImagesItem(
                nodeId = 1,
                nodeImages = NodeImagesItem("entity1", 200, 100, "url1"),
                isRestrictedAge18 = true
            )
        )
    )

    val NODE_IMAGES_DTO_2 = CmsNavigationNodeImagesDto(
        id = 2,
        images = listOf(NavigationNodeImagesDto(nodeId = 3, imageDto = null, isRestrictedAge18 = false))
    )

    val EXPECTED_NODE_IMAGES_EXCEPTIONAL_2 = NavigationNodeImages(
        listOf(
            NavigationNodeImagesItem(
                nodeId = 3,
                nodeImages = NodeImagesItem(null, null, null, null),
                isRestrictedAge18 = false
            )
        )
    )

    val NODE_IMAGES_DTO_3 = CmsNavigationNodeImagesDto(
        id = 3,
        images = listOf(
            NavigationNodeImagesDto(
                nodeId = 111,
                imageDto = NodeImageDto("entity111", 111, 111, "url111"),
                isRestrictedAge18 = false
            ),
            NavigationNodeImagesDto(
                nodeId = 222,
                imageDto = NodeImageDto(null, 222, 222, "url222"),
                isRestrictedAge18 = true
            ),
            NavigationNodeImagesDto(
                nodeId = 333,
                imageDto = NodeImageDto("entity333", null, null, null),
                isRestrictedAge18 = false
            ),
            NavigationNodeImagesDto(
                nodeId = 444,
                imageDto = null,
                isRestrictedAge18 = true
            )
        )
    )

    val EXPECTED_NODE_IMAGES_EXCEPTIONAL_3 = NavigationNodeImages(
        listOf(
            NavigationNodeImagesItem(
                nodeId = 111,
                nodeImages = NodeImagesItem("entity111", 111, 111, "url111"),
                isRestrictedAge18 = false
            ),
            NavigationNodeImagesItem(
                nodeId = 222,
                nodeImages = NodeImagesItem(null, 222, 222, "url222"),
                isRestrictedAge18 = true
            ),
            NavigationNodeImagesItem(
                nodeId = 333,
                nodeImages = NodeImagesItem("entity333", null, null, null),
                isRestrictedAge18 = false
            ),
            NavigationNodeImagesItem(
                nodeId = 444,
                nodeImages = NodeImagesItem(null, null, null, null),
                isRestrictedAge18 = true
            )
        )
    )
}