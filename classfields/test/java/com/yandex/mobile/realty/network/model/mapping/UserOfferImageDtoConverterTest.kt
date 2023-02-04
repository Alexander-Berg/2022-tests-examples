package com.yandex.mobile.realty.network.model.mapping

import com.yandex.mobile.realty.data.mapping.EmptyDescriptor
import com.yandex.mobile.realty.data.model.publication.UserOfferImageDto
import com.yandex.mobile.realty.data.model.publication.UserOfferImageDto.Variant
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * @author andrey-bgm on 17/02/2020.
 */
class UserOfferImageDtoConverterTest {

    @Test
    fun convert() {
        val dto = UserOfferImageDto(
            "avatars/ef115",
            listOf(
                Variant("avatars/ef115/large", "large"),
                Variant("avatars/ef115/app_snippet_large", "app_snippet_large"),
                Variant("avatars/ef115/app_snippet_middle", "app_snippet_middle"),
                Variant("avatars/ef115/app_snippet_small", "app_snippet_small"),
                Variant("avatars/ef115/app_snippet_mini", "app_snippet_mini")
            )
        )

        val userOfferImage = UserOfferImageDto.CONVERTER.map(dto, EmptyDescriptor)

        assertEquals("avatars/ef115", userOfferImage.id)
        assertEquals("avatars/ef115/large", userOfferImage.image.full)
        assertEquals("avatars/ef115/app_snippet_large", userOfferImage.image.xxHigh)
        assertEquals("avatars/ef115/app_snippet_middle", userOfferImage.image.xHigh)
        assertEquals("avatars/ef115/app_snippet_small", userOfferImage.image.high)
        assertEquals("avatars/ef115/app_snippet_mini", userOfferImage.image.medium)
    }
}
