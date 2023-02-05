package ru.yandex.market.clean.data.mapper.selector.collection.startnode

import com.annimon.stream.Exceptional
import org.assertj.core.api.Assertions
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.yandex.market.clean.data.mapper.ImageMapper
import ru.yandex.market.clean.data.model.dto.cms.selector.content.node.selectorStartNodeDtoTestInstance
import ru.yandex.market.clean.data.model.dto.cms.selector.picture.pictureDtoTestInstance
import ru.yandex.market.clean.domain.model.selector.collection.SelectorNode
import ru.yandex.market.domain.media.model.EmptyImageReference

class SelectorStartNodeMapperTest {

    private val image = EmptyImageReference()
    private val imageMapperMock = mock<ImageMapper>() {
        on { mapImage(IMAGE_URL, false) } doReturn Exceptional.of { image }
    }
    private val mapper = SelectorStartNodeMapper(imageMapperMock)

    @Test
    fun map() {
        val pictureDto = pictureDtoTestInstance(url = IMAGE_URL)

        val expectedResult = SelectorNode.StartNode(
            label = "label",
            negativeButtonText = "denyButtonText",
            next = firstFlowNode,
            picture = image,
            positiveButtonText = "confirmButtonText"
        )

        val actualResult = mapper.map(selectorStartNodeDtoTestInstance(picture = pictureDto), firstFlowNode)

        Assertions.assertThat(actualResult).isEqualTo(expectedResult)
    }

    private companion object {
        const val IMAGE_URL = "image_url"
        val firstFlowNode = SelectorNode.FlowNode(
            chips = emptyList(),
            label = "",
            next = null
        )
    }
}