package ru.yandex.market.clean.data.repository.mapper.navigationnode

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import ru.yandex.market.clean.data.mapper.ImageMapper
import ru.yandex.market.clean.data.repository.mapper.navigationnode.NavigationNodeDataMapperTestEntity.EXPECTED_RESULT
import ru.yandex.market.clean.data.repository.mapper.navigationnode.NavigationNodeDataMapperTestEntity.INPUT_NODE_DTO
import ru.yandex.market.util.extensions.convertToString

class NavigationNodeDataMapperTest {

    private val imageMapper = mock<ImageMapper>()
    private val mapper = NavigationNodeDataMapper(imageMapper)

    @Test
    fun `check correct mapping`() {
        val actualResult = mapper.map(INPUT_NODE_DTO, 0)
        assertThat(actualResult.convertToString()).isEqualTo(EXPECTED_RESULT.convertToString())
    }
}