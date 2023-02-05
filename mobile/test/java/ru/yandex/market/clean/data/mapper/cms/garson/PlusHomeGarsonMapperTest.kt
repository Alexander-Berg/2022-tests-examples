package ru.yandex.market.clean.data.mapper.cms.garson

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.yandex.market.clean.data.model.dto.cms.garson.PictureDto
import ru.yandex.market.clean.data.model.dto.cms.garson.PlusHomeGarsonDto
import ru.yandex.market.clean.data.model.dto.cms.garson.pictureDtoTestInstance
import ru.yandex.market.clean.data.model.dto.cms.garson.plusHomeGarsonDtoTestInstance
import ru.yandex.market.clean.domain.model.cms.garson.PlusHomeGarson
import ru.yandex.market.clean.domain.model.cms.garson.plusHomeGarsonTestInstance
import ru.yandex.market.domain.media.model.measuredImageReferenceTestInstance

@RunWith(Parameterized::class)
class PlusHomeGarsonMapperTest(
    private val input: PlusHomeGarsonDto,
    private val expected: PlusHomeGarson?
) {

    private val garsonPictureMapper = mock<GarsonPictureMapper> {
        on { mapImage(VALID_PICTURE) } doReturn measuredImageReferenceTestInstance()
        on { mapImage(INVALID_PICTURE) } doReturn null
    }

    private val plusHomeGarsonMapper = PlusHomeGarsonMapper(garsonPictureMapper)

    @Test
    fun testMap() {
        val actual = plusHomeGarsonMapper.map(input)

        assertThat(actual).isEqualTo(expected)
    }

    companion object {
        private val VALID_PICTURE = pictureDtoTestInstance()
        private val INVALID_PICTURE = mock<PictureDto>()

        @Parameterized.Parameters(name = "{index}: {0} -> {1}")
        @JvmStatic
        fun data(): Iterable<Array<*>> = listOf(
            //0
            arrayOf(
                plusHomeGarsonDtoTestInstance(showTo = listOf("PLUS", "REGULAR", "NOT_LOGGED_IN")),
                plusHomeGarsonTestInstance(
                    showTo = listOf(
                        PlusHomeGarson.UserType.PLUS,
                        PlusHomeGarson.UserType.REGULAR,
                        PlusHomeGarson.UserType.NOT_LOGGED_IN
                    )
                )
            ),
            //1
            arrayOf(
                plusHomeGarsonDtoTestInstance(showTo = listOf("PLUS", "TEST", "UNKNOWN", "YANDEX")),
                plusHomeGarsonTestInstance(showTo = listOf(PlusHomeGarson.UserType.PLUS))
            ),
            //2
            arrayOf(
                plusHomeGarsonDtoTestInstance(showTo = listOf("wtf", "some_user", "")),
                plusHomeGarsonTestInstance(showTo = emptyList())
            ),
            //3
            arrayOf(
                plusHomeGarsonDtoTestInstance(showTo = emptyList()),
                plusHomeGarsonTestInstance(showTo = emptyList())
            ),
            //4
            arrayOf(
                plusHomeGarsonDtoTestInstance(title = null),
                null
            ),
            //5
            arrayOf(
                plusHomeGarsonDtoTestInstance(subtitle = null),
                null
            ),
            //6
            arrayOf(
                plusHomeGarsonDtoTestInstance(button = null),
                null
            ),
            //7
            arrayOf(
                plusHomeGarsonDtoTestInstance(tag = null),
                null
            ),
            //8
            arrayOf(
                plusHomeGarsonDtoTestInstance(showTo = null),
                null
            ),
            //9
            arrayOf(
                plusHomeGarsonDtoTestInstance(icon = null),
                null
            ),
            //10
            arrayOf(
                plusHomeGarsonDtoTestInstance(icon = INVALID_PICTURE),
                null
            ),
        )
    }
}