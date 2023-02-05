package ru.yandex.market.clean.data.mapper.selector.collection.flownode

import org.assertj.core.api.Assertions
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.yandex.market.clean.data.model.dto.cms.selector.content.node.chip.selectorChipDtoTestInstance
import ru.yandex.market.clean.data.model.dto.cms.selector.content.node.selectorNodeDtoTestInstance
import ru.yandex.market.clean.domain.model.selector.collection.SelectorNode
import ru.yandex.market.clean.domain.model.selector.collection.SelectorCollection
import ru.yandex.market.data.filters.filter.TextFilter

class SelectorFlowNodeMapperTest {

    private val textFilterMapper = mock<SelectorFilterMapper>() {
        on { map(any()) } doReturn TextFilter()
    }

    private val mapper = SelectorFlowNodeMapper(textFilterMapper)

    private val pendingNode = SelectorNode.PendingNode(
        next = mock(),
        pendingPost = mock()
    )
    private val firstLinearFlowNode = SelectorNode.FlowNode(
        chips = emptyList(),
        next = pendingNode,
        label = "label"
    )
    private val firstTreeFlowNode = SelectorNode.FlowNode(
        chips = listOf(
            SelectorNode.Chip(
                filters = emptyList(),
                hashCodeId = 0,
                isSelected = false,
                label = "label",
                nextNode = SelectorNode.FlowNode(
                    chips = emptyList(),
                    label = "label",
                    next = null
                ),
                hid = CHIP_HID,
                nid = CHIP_NID
            )
        ),
        next = null,
        label = "label"
    )

    @Test
    fun `test map linear scenario type`() {
        val expectedResult = firstLinearFlowNode
        val actualResult = mapper.map(
            SelectorCollection.Type.LINEAR,
            listOf(selectorNodeDtoTestInstance(chips = emptyList())),
            pendingNode
        )

        Assertions.assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun `test map tree scenario type`() {
        val expectedResult = firstTreeFlowNode
        var actualResult = mapper.map(
            SelectorCollection.Type.TREE,
            listOf(
                selectorNodeDtoTestInstance(
                    chips = listOf(
                        selectorChipDtoTestInstance(
                            nextNode = selectorNodeDtoTestInstance(chips = emptyList()),
                            filters = emptyList(),
                            hid = CHIP_HID,
                            nid = CHIP_NID
                        )
                    )
                )
            ),
            pendingNode
        )
        actualResult = actualResult.copy(
            chips = actualResult.chips.map { it.copy(hashCodeId = 0) }
        )

        Assertions.assertThat(actualResult).isEqualTo(expectedResult)
    }

    private companion object {
        const val CHIP_HID = "123456"
        const val CHIP_NID = "654321"
    }
}