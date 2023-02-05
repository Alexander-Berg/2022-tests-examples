package ru.yandex.market.data.media.image.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import ru.yandex.market.data.media.image.avatars.AvatarsUrlParser
import ru.yandex.market.domain.media.model.AvatarsImageReference
import ru.yandex.market.domain.media.model.SimpleImageReference

class ImageReferenceMapperTest {

    @Test
    fun `Method 'mapImage(url, isRestrictedAge18)' handles avatars url with first priority`() {
        val url = "my-url"
        val avatarsUrlParser = mock<AvatarsUrlParser> {
            on { parse(url) } doReturn AvatarsUrlParser.Result(
                namespace = "testNamespace",
                groupId = 42,
                imageKey = "testImage",
                quality = null
            )
        }
        val mapper = ImageReferenceMapper(avatarsUrlParser, emptyList())
        val imageReference = mapper.mapImage(url, false)
        assertThat(imageReference).isInstanceOf(AvatarsImageReference::class.java)
    }

    @Test
    fun `Method 'mapImage(url, isRestrictedAge18)' handles non-avatars url as SimpleImageReference`() {
        val url = "my-url"
        val mapper = ImageReferenceMapper(mock(), emptyList())
        val imageReference = mapper.mapImage(url, false)
        assertThat(imageReference).isInstanceOf(SimpleImageReference::class.java)
    }

    @Test
    fun `Method 'mapImage(url, isRestrictedAge18)' pass url to SimpleImageReference as is if there is no patchers`() {
        val url = "my-url"
        val mapper = ImageReferenceMapper(mock(), emptyList())
        val imageReference = mapper.mapImage(url, false)
        val urlFromReference = (imageReference as SimpleImageReference).url
        assertThat(urlFromReference).isEqualTo(url)
    }

    @Test
    fun `Method 'mapImage(url, isRestrictedAge18)' applies patchers to url before pass to SimpleImageReference`() {
        val url = "my-url"
        val httpsSchemaPatcher = object : ImageUrlPatcher {
            override fun shouldPatch(url: String): Boolean = !url.startsWith("https://")
            override fun applyPatch(url: String): String = "https://$url"
        }
        val userIdPatcher = object : ImageUrlPatcher {
            override fun shouldPatch(url: String): Boolean = true
            override fun applyPatch(url: String): String = "$url?userId=42"
        }
        val mapper = ImageReferenceMapper(mock(), listOf(httpsSchemaPatcher, userIdPatcher))
        val imageReference = mapper.mapImage(url, false)
        val urlFromReference = (imageReference as SimpleImageReference).url
        assertThat(urlFromReference).startsWith("https://").endsWith("?userId=42")
    }

    @Test
    fun `Method 'mapSimpleImage(url, isRestrictedAge18)' pass url to SimpleImageReference as is if there is no patchers`() {
        val url = "my-url"
        val mapper = ImageReferenceMapper(mock(), emptyList())
        val imageReference = mapper.mapSimpleImage(url, false)
        assertThat(imageReference.url).isEqualTo(url)
    }

    @Test
    fun `Method 'mapSimpleImage(url, isRestrictedAge18)' applies patchers to url before pass to SimpleImageReference`() {
        val url = "my-url"
        val httpsSchemaPatcher = object : ImageUrlPatcher {
            override fun shouldPatch(url: String): Boolean = !url.startsWith("https://")
            override fun applyPatch(url: String): String = "https://$url"
        }
        val userIdPatcher = object : ImageUrlPatcher {
            override fun shouldPatch(url: String): Boolean = true
            override fun applyPatch(url: String): String = "$url?userId=42"
        }
        val mapper = ImageReferenceMapper(mock(), listOf(httpsSchemaPatcher, userIdPatcher))
        val imageReference = mapper.mapSimpleImage(url, false)
        assertThat(imageReference.url).startsWith("https://").endsWith("?userId=42")
    }

    @Test
    fun `Method 'mapAvatarsImage(url, isRestrictedAge18)' applies patchers to url before pass to parser`() {
        val url = "my-url"
        val avatarsUrlParser = mock<AvatarsUrlParser>()
        val patcher = object : ImageUrlPatcher {
            override fun shouldPatch(url: String): Boolean = true
            override fun applyPatch(url: String): String = "stubbed-url"
        }
        val mapper = ImageReferenceMapper(avatarsUrlParser, listOf(patcher))
        mapper.mapAvatarsImage(url, false)
        verify(avatarsUrlParser, never()).parse(url)
        verify(avatarsUrlParser).parse("stubbed-url")
    }
}