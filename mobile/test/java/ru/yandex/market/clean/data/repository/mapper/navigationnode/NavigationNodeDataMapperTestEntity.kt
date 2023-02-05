package ru.yandex.market.clean.data.repository.mapper.navigationnode

import ru.yandex.market.clean.data.model.dto.DataSourceType
import ru.yandex.market.clean.data.model.dto.NavigationDataSourceDto
import ru.yandex.market.clean.data.model.dto.NavigationNodeDto
import ru.yandex.market.clean.data.model.dto.cms.CmsImageDto
import ru.yandex.market.clean.domain.model.NavigationNode
import ru.yandex.market.data.navigation.NodeType
import ru.yandex.market.data.navigation.SimplifiedFilterValue
import ru.yandex.market.domain.media.model.MeasuredImageReference
import ru.yandex.market.net.Sort
import ru.yandex.market.net.sku.fapi.dto.pictureDtoTestInstance

object NavigationNodeDataMapperTestEntity {

    private val CHILD_DATA_SOURCE_DTO = NavigationDataSourceDto(
        type = DataSourceType.CATALOG,
        hid = "hid",
        nid = "nid",
        sort = Sort.DEFAULT,
        simplifiedFilterValues = listOf(SimplifiedFilterValue("childSourceDtoId", "childSourceDtoValue"))
    )

    private val CHILD_NODE_DTO = NavigationNodeDto(
        "childId",
        "childHid",
        "childName",
        "",
        emptyList(),
        NodeType.CATEGORY,
        emptyList(),
        emptyList(),
        CHILD_DATA_SOURCE_DTO,
        null,
        false,
        false,
        listOf("tag1, tag2"),
    )

    private val PARENT_DATA_SOURCE_DTO = NavigationDataSourceDto(
        type = DataSourceType.CATALOGLEAF,
        hid = "hid",
        nid = "nid",
        sort = Sort.BY_POPULARITY,
        simplifiedFilterValues = listOf(SimplifiedFilterValue("parentSourceDtoId", "parentSourceDtoValue"))
    )

    val INPUT_NODE_DTO = NavigationNodeDto(
        "id",
        "hid",
        "name",
        "shortName",
        listOf(CHILD_NODE_DTO),
        NodeType.CATEGORY,
        listOf(CmsImageDto(width = 250, height = 100, url = "testUrl", alt = null)),
        listOf(pictureDtoTestInstance()),
        PARENT_DATA_SOURCE_DTO,
        null,
        true,
        true,
        listOf("tag1, tag2"),
    )

    private val CHILD_NODE = NavigationNode(
        id = CHILD_NODE_DTO.id,
        categoryId = CHILD_NODE_DTO.hid!!,
        name = CHILD_NODE_DTO.fullName!!,
        childNodes = emptyList(),
        image = MeasuredImageReference.empty(),
        isHasImage = true,
        sort = Sort.DEFAULT,
        filters = mapOf("childSourceDtoId" to "childSourceDtoValue"),
        isLeaf = CHILD_NODE_DTO.isLeaf,
        isDepartment = CHILD_NODE_DTO.isDepartment,
        hid = CHILD_NODE_DTO.hid!!,
        nid = CHILD_NODE_DTO.nid,
        dataSourceType = DataSourceType.CATALOG,
        isFromCache = false,
        parentId = null,
        parentNode = null,
        tags = listOf("tag1, tag2"),
    )

    val EXPECTED_RESULT = NavigationNode(
        id = INPUT_NODE_DTO.id,
        categoryId = INPUT_NODE_DTO.hid!!,
        name = INPUT_NODE_DTO.shortName!!,
        childNodes = listOf(CHILD_NODE),
        image = MeasuredImageReference.empty(),
        isHasImage = true,
        sort = Sort.BY_POPULARITY,
        filters = mapOf("parentSourceDtoId" to "parentSourceDtoValue"),
        isLeaf = INPUT_NODE_DTO.isLeaf,
        isDepartment = INPUT_NODE_DTO.isDepartment,
        hid = INPUT_NODE_DTO.hid!!,
        nid = INPUT_NODE_DTO.nid,
        dataSourceType = DataSourceType.CATALOGLEAF,
        isFromCache = false,
        parentId = null,
        parentNode = null,
        tags = listOf("tag1, tag2"),
    )
}
