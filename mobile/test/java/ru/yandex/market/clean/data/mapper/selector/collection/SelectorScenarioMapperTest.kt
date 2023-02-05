package ru.yandex.market.clean.data.mapper.selector.collection

import org.assertj.core.api.Assertions
import org.junit.Test

import org.junit.Assert.*
import ru.yandex.market.clean.data.model.dto.cms.selector.content.node.SelectorNodeDto
import ru.yandex.market.clean.data.model.dto.cms.selector.content.selectorScenarioDtoTestInstance
import ru.yandex.market.clean.domain.model.selector.collection.SelectorCollection

class SelectorScenarioMapperTest {

    private val mapper = SelectorScenarioMapper()

    @Test
    fun `map list collection type`() {
        val dtoList = listOf(nodeDto)
        val dto = selectorScenarioDtoTestInstance(LINEAR_TYPE, listOf(nodeDto))

        val expectedResult = SelectorCollection.Type.LINEAR to dtoList

        val actualResult = mapper.map(dto)

        Assertions.assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun `map tree collection type`() {
        val dtoList = listOf(nodeDto)
        val dto = selectorScenarioDtoTestInstance(TREE_TYPE, listOf(nodeDto))

        val expectedResult = SelectorCollection.Type.TREE to dtoList

        val actualResult = mapper.map(dto)

        Assertions.assertThat(actualResult).isEqualTo(expectedResult)
    }

    private companion object {
        const val LINEAR_TYPE = "linear"
        const val TREE_TYPE = "tree"
        val nodeDto = SelectorNodeDto(
            chips = emptyList(),
            label = "some string"
        )
    }
}