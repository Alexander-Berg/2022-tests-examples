package ru.yandex.market.clean.data.mapper.selector

import org.assertj.core.api.Assertions
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.yandex.market.clean.data.mapper.selector.collection.SelectorScenarioMapper
import ru.yandex.market.clean.data.mapper.selector.collection.endnode.SelectorEndNodeMapper
import ru.yandex.market.clean.data.mapper.selector.collection.flownode.SelectorFlowNodeMapper
import ru.yandex.market.clean.data.mapper.selector.collection.startnode.SelectorStartNodeMapper
import ru.yandex.market.clean.data.mapper.selector.response.SelectorFlowMapper
import ru.yandex.market.clean.data.mapper.selector.response.SelectorVisibilityMapper
import ru.yandex.market.clean.data.model.dto.cms.selector.content.node.selectorNodeDtoTestInstance
import ru.yandex.market.clean.data.model.dto.cms.selector.content.selectorContentDtoTestInstance
import ru.yandex.market.clean.data.model.dto.cms.selector.content.selectorScenarioDtoTestInstance
import ru.yandex.market.clean.data.model.dto.cms.selector.selectorFlowDtoTestInstance
import ru.yandex.market.clean.domain.model.selector.response.ImagePost
import ru.yandex.market.clean.domain.model.selector.response.SelectorEntryPoints
import ru.yandex.market.clean.domain.model.selector.collection.SelectorNode
import ru.yandex.market.clean.domain.model.selector.collection.SelectorCollection
import ru.yandex.market.domain.media.model.EmptyImageReference

class SelectorFlowMapperTest {

    private val flowNode = SelectorNode.FlowNode(
        chips = emptyList(),
        label = "",
        next = null
    )

    private val startNodeMapperMock = mock<SelectorStartNodeMapper>() {
        on { map(any(), any()) } doReturn SelectorNode.StartNode(
            label = "",
            positiveButtonText = "",
            negativeButtonText = "",
            picture = EmptyImageReference(),
            next = flowNode
        )
    }

    private val endNodeMapperMock = mock<SelectorEndNodeMapper>() {
        on { map(any()) } doReturn SelectorNode.PendingNode(
            next = mock(),
            pendingPost = ImagePost("", EmptyImageReference())
        )
    }

    private val flowNodeMapperMock = mock<SelectorFlowNodeMapper>() {
        on { map(any(), any(), any()) } doReturn flowNode
    }

    private val entryPointsVisibilityMapperMock = mock<SelectorVisibilityMapper>() {
        on { map(any()) } doReturn SelectorEntryPoints.default()
    }

    @Test
    fun `test map linear flow`() {
        val scenarioDto = selectorScenarioDtoTestInstance(
            type = "linear",
            nodes = listOf(selectorNodeDtoTestInstance(chips = emptyList()))
        )
        val selectorFlowDto = selectorFlowDtoTestInstance(
            content = selectorContentDtoTestInstance(scenario = scenarioDto),
        )
        val scenarioMapperMock = mock<SelectorScenarioMapper>() {
            on { map(scenarioDto) } doReturn (
                    SelectorCollection.Type.LINEAR to listOf(selectorNodeDtoTestInstance(chips = emptyList()))
                    )
        }
        val mapper = SelectorFlowMapper(
            scenarioMapperMock,
            startNodeMapperMock,
            endNodeMapperMock,
            flowNodeMapperMock,
            entryPointsVisibilityMapperMock
        )

        val expectedResult = SelectorCollection.Type.LINEAR

        val actualResult = mapper.mapConfig(selectorFlowDto).selectorCollection.type

        Assertions.assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun `test map tree flow`() {
        val scenarioDto = selectorScenarioDtoTestInstance(
            type = "tree",
            nodes = listOf(selectorNodeDtoTestInstance(chips = emptyList()))
        )
        val selectorFlowDto = selectorFlowDtoTestInstance(
            content = selectorContentDtoTestInstance(scenario = scenarioDto)
        )
        val scenarioMapperMock = mock<SelectorScenarioMapper>() {
            on { map(scenarioDto) } doReturn (
                    SelectorCollection.Type.TREE to listOf(selectorNodeDtoTestInstance(chips = emptyList()))
                    )
        }
        val mapper = SelectorFlowMapper(
            scenarioMapperMock,
            startNodeMapperMock,
            endNodeMapperMock,
            flowNodeMapperMock,
            entryPointsVisibilityMapperMock
        )

        val expectedResult = SelectorCollection.Type.TREE

        val actualResult = mapper.mapConfig(selectorFlowDto).selectorCollection.type

        Assertions.assertThat(actualResult).isEqualTo(expectedResult)
    }
}