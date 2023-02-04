package com.yandex.mobile.realty.network.model.mapping

import com.yandex.mobile.realty.data.mapping.EmptyDescriptor
import com.yandex.mobile.realty.network.InvalidDtoException
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * @author sorokinandrei on 12/3/20.
 */
class YouTubeVideoIdConverterTest {

    @Test
    fun shouldParseShortenedUrl() {
        val videoId = Converters.videoIdConverter.map(
            "https://youtu.be/8UZFoOp_vk0",
            EmptyDescriptor
        )
        assertEquals(videoId, "8UZFoOp_vk0")
    }

    @Test
    fun shouldParseEmbedUrl() {
        val videoId = Converters.videoIdConverter.map(
            "https://www.youtube.com/embed/uPtUhbvnW_U?rel=0",
            EmptyDescriptor
        )
        assertEquals(videoId, "uPtUhbvnW_U")
    }

    @Test
    fun shouldParseWatchUrl() {
        val videoId = Converters.videoIdConverter.map(
            "https://www.youtube.com/watch?v=uPtUhbvnW_U",
            EmptyDescriptor
        )
        assertEquals(videoId, "uPtUhbvnW_U")
    }

    @Test(expected = InvalidDtoException::class)
    fun shouldNotParseWrongUrl() {
        Converters.videoIdConverter.map(
            "https://www.t.com/watch?v=uPtUhbvnW_U",
            EmptyDescriptor
        )
    }
}
