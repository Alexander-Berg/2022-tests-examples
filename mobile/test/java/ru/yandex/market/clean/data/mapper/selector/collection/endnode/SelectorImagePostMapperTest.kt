package ru.yandex.market.clean.data.mapper.selector.collection.endnode

import com.annimon.stream.Exceptional
import org.assertj.core.api.Assertions
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.yandex.market.clean.data.mapper.ImageMapper
import ru.yandex.market.clean.data.model.dto.cms.selector.picture.pictureDtoTestInstance
import ru.yandex.market.clean.domain.model.selector.response.ImagePost
import ru.yandex.market.domain.media.model.EmptyImageReference

class SelectorImagePostMapperTest {

    private val image = EmptyImageReference()
    private val imageMapper = mock<ImageMapper>() {
        on { mapImage(IMAGE_URL, false) } doReturn Exceptional.of { image }
    }
    private val mapper = SelectorImagePostMapper(imageMapper)

    @Test
    fun map() {
        val pictureDto = pictureDtoTestInstance(url = IMAGE_URL)

        val expectedResult = ImagePost(LABEL, image)

        val actualResult = mapper.map(LABEL, pictureDto)

        Assertions.assertThat(actualResult).isEqualTo(expectedResult)
    }

    private companion object {
        const val IMAGE_URL = "image_url"
        const val LABEL = "label"
    }
}