package ru.yandex.market.clean.data.mapper.cms.garson

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.yandex.market.clean.data.model.dto.cms.garson.pictureDtoTestInstance
import ru.yandex.market.clean.data.model.dto.cms.garson.referralProgramGarsonDtoTestInstance
import ru.yandex.market.clean.domain.model.cms.garson.referralProgramGarsonTestInstance
import ru.yandex.market.domain.media.model.MeasuredImageReference
import ru.yandex.market.domain.media.model.measuredImageReferenceTestInstance

class ReferralProgramGarsonMapperTest {

    private val pictureMapper = mock<GarsonPictureMapper> {
        on { mapImage(pictureDtoTestInstance(url = PICTURE_URL)) } doReturn measuredImageReferenceTestInstance()
    }

    private val mapper = ReferralProgramGarsonMapper(pictureMapper)

    @Test
    fun `map garson with image`() {
        val dto = referralProgramGarsonDtoTestInstance(icon = pictureDtoTestInstance(url = PICTURE_URL))
        val expected = referralProgramGarsonTestInstance(icon = measuredImageReferenceTestInstance())
        assertThat(mapper.map(dto)).isEqualTo(expected)
    }

    @Test
    fun `map garson without image`() {
        val dto = referralProgramGarsonDtoTestInstance(icon = null)
        val expected = referralProgramGarsonTestInstance(icon = MeasuredImageReference.empty())
        assertThat(mapper.map(dto)).isEqualTo(expected)
    }

    companion object {
        private const val PICTURE_URL = "picture url"
    }
}