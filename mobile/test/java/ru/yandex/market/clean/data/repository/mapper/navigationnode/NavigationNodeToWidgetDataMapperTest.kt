package ru.yandex.market.clean.data.repository.mapper.navigationnode

import com.annimon.stream.Exceptional
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import ru.yandex.market.clean.data.mapper.ImageMapper
import ru.yandex.market.clean.data.repository.mapper.navigationnode.NavigationNodeToWidgetDataMapperTestEntity.EXPECTED_WIDGET_DATA
import ru.yandex.market.clean.data.repository.mapper.navigationnode.NavigationNodeToWidgetDataMapperTestEntity.INPUT_IMAGES
import ru.yandex.market.clean.data.repository.mapper.navigationnode.NavigationNodeToWidgetDataMapperTestEntity.INPUT_ROOT_NODE
import ru.yandex.market.domain.media.model.measuredImageReferenceTestInstance
import ru.yandex.market.mockResult

class NavigationNodeToWidgetDataMapperTest {

    private val imageMapper = mock<ImageMapper>()
    private val mapper = NavigationNodeToWidgetDataMapper(imageMapper)

    @Before
    fun setUp() {
        imageMapper.mapMeasuredImage(any(), any(), any(), anyOrNull(), any())
            .mockResult(Exceptional.of { measuredImageReferenceTestInstance(url = "imageUrl2") })
    }

    @Test
    fun `check correct mapping`() {
        val actualResult = mapper.map(INPUT_ROOT_NODE, INPUT_IMAGES)
        assertThat(actualResult).isEqualTo(EXPECTED_WIDGET_DATA)
    }
}