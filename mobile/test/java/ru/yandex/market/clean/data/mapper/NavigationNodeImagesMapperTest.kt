package ru.yandex.market.clean.data.mapper

import com.annimon.stream.Exceptional
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.market.clean.data.mapper.NavigationNodeImagesMapperTestEntity.EXPECTED_NODE_IMAGES_EXCEPTIONAL_1
import ru.yandex.market.clean.data.mapper.NavigationNodeImagesMapperTestEntity.EXPECTED_NODE_IMAGES_EXCEPTIONAL_2
import ru.yandex.market.clean.data.mapper.NavigationNodeImagesMapperTestEntity.EXPECTED_NODE_IMAGES_EXCEPTIONAL_3
import ru.yandex.market.clean.data.mapper.NavigationNodeImagesMapperTestEntity.NODE_IMAGES_DTO_1
import ru.yandex.market.clean.data.mapper.NavigationNodeImagesMapperTestEntity.NODE_IMAGES_DTO_2
import ru.yandex.market.clean.data.mapper.NavigationNodeImagesMapperTestEntity.NODE_IMAGES_DTO_3
import ru.yandex.market.clean.data.model.dto.cms.CmsNavigationNodeImagesDto
import ru.yandex.market.clean.domain.model.NavigationNodeImages
import ru.yandex.market.util.extensions.convertToString

@RunWith(Parameterized::class)
class NavigationNodeImagesMapperTest(
    private val inputDto: CmsNavigationNodeImagesDto?,
    private val expectedResult: Exceptional<NavigationNodeImages>
) {

    private val mapper = NavigationNodeImagesMapper()

    @Test
    fun `check dto images mapping`() {
        assertThat(mapper.map(inputDto).convertToString()).isEqualTo(expectedResult.convertToString())
    }

    companion object {

        @Parameterized.Parameters
        @JvmStatic
        fun parameters() = listOf(
            arrayOf(null, Exceptional.of<Throwable>(IllegalArgumentException("images не должно быть равно null"))),
            arrayOf(NODE_IMAGES_DTO_1, Exceptional.of { EXPECTED_NODE_IMAGES_EXCEPTIONAL_1 }),
            arrayOf(NODE_IMAGES_DTO_2, Exceptional.of { EXPECTED_NODE_IMAGES_EXCEPTIONAL_2 }),
            arrayOf(NODE_IMAGES_DTO_3, Exceptional.of { EXPECTED_NODE_IMAGES_EXCEPTIONAL_3 })
        )
    }
}