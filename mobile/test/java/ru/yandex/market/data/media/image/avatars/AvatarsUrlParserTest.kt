package ru.yandex.market.data.media.image.avatars

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AvatarsUrlParserTest {

    private val parser = AvatarsUrlParser()

    @Test
    fun `Returns correct result for valid url`() {
        val namespace = "myspace"
        val groupId = 23657235
        val imageKey = "img_id842395"
        val quality = "myQuality"
        val url = "https://avatars.mds.yandex.net/get-${namespace}/${groupId}/${imageKey}/${quality}"
        val result = parser.parse(url)
        requireNotNull(result)
        assertThat(result.namespace).isEqualTo(namespace)
        assertThat(result.groupId).isEqualTo(groupId)
        assertThat(result.imageKey).isEqualTo(imageKey)
        assertThat(result.quality).isEqualTo(quality)
    }

    @Test
    fun `Returns correct result for url without schema`() {
        val namespace = "myspace"
        val groupId = 23657235
        val imageKey = "img_id842395"
        val quality = "myQuality"
        val url = "https://avatars.mds.yandex.net/get-${namespace}/${groupId}/${imageKey}/${quality}"
        val result = parser.parse(url)
        requireNotNull(result)
        assertThat(result.namespace).isEqualTo(namespace)
        assertThat(result.groupId).isEqualTo(groupId)
        assertThat(result.imageKey).isEqualTo(imageKey)
        assertThat(result.quality).isEqualTo(quality)
    }

    @Test
    fun `Returns correct result for url without quality`() {
        val namespace = "myspace"
        val groupId = 23657235
        val imageKey = "img_id842395"
        val url = "//avatars.mds.yandex.net/get-${namespace}/${groupId}/${imageKey}/"
        val result = parser.parse(url)
        requireNotNull(result)
        assertThat(result.namespace).isEqualTo(namespace)
        assertThat(result.groupId).isEqualTo(groupId)
        assertThat(result.imageKey).isEqualTo(imageKey)
        assertThat(result.quality).isEqualTo(null)
    }

    @Test
    fun `Returns null result for url with wrong schema`() {
        val namespace = "myspace"
        val groupId = 23657235
        val imageKey = "img_id842395"
        val quality = "myQuality"
        val url = "http://avatars.mds.yandex.net/get-${namespace}/${groupId}/${imageKey}/${quality}"
        val result = parser.parse(url)
        assertThat(result).isNull()
    }

    @Test
    fun `Returns null result for url with wrong host`() {
        val namespace = "myspace"
        val groupId = 23657235
        val imageKey = "img_id842395"
        val quality = "myQuality"
        val url = "https://this-is-not-avatars.mds.yandex.net/get-${namespace}/${groupId}/${imageKey}/${quality}"
        val result = parser.parse(url)
        assertThat(result).isNull()
    }

    @Test
    fun `Returns null result for url without path`() {
        val url = "http://avatars.mds.yandex.net/"
        val result = parser.parse(url)
        assertThat(result).isNull()
    }
}