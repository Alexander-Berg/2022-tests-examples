package ru.yandex.disk.video

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.nullValue
import org.junit.Test
import ru.yandex.disk.test.AndroidTestCase2

class VideoResolutionParseTest : AndroidTestCase2() {

    @Test
    fun `match test`() {
        assertThat(VideoResolution.match("1080"), equalTo(VideoResolution.p1080))
        assertThat(VideoResolution.match("720"), equalTo(VideoResolution.p720))
        assertThat(VideoResolution.match("480"), equalTo(VideoResolution.p480))
        assertThat(VideoResolution.match("360"), equalTo(VideoResolution.p360))
        assertThat(VideoResolution.match("240"), equalTo(VideoResolution.p240))
        assertThat(VideoResolution.match("721"), equalTo(VideoResolution.p720))
        assertThat(VideoResolution.match("719"), equalTo(VideoResolution.p480))
        assertThat(VideoResolution.match("481"), equalTo(VideoResolution.p480))
        assertThat(VideoResolution.match("479"), equalTo(VideoResolution.p360))
        assertThat(VideoResolution.match("nosense"), equalTo(VideoResolution.p240))
        assertThat(VideoResolution.match("adaptive"), equalTo(VideoResolution.ADAPTIVE))
        assertThat(VideoResolution.match("original"), equalTo(VideoResolution.ORIGINAL))
    }

    @Test
    fun `parse test`() {
        assertThat(VideoResolution.parse("1920x1080"), equalTo(VideoResolution.p1080))
        assertThat(VideoResolution.parse("1280x720"), equalTo(VideoResolution.p720))
        assertThat(VideoResolution.parse("60x359"), equalTo(VideoResolution.p240))
        assertThat(VideoResolution.parse("360x720"), equalTo(VideoResolution.p720))
        assertThat(VideoResolution.parse("1280p720"), equalTo(VideoResolution.p240))
        assertThat(VideoResolution.parse("nosense"), equalTo(VideoResolution.p240))
        assertThat(VideoResolution.parse("adaptive"), equalTo(VideoResolution.ADAPTIVE))
        assertThat(VideoResolution.parse("original"), equalTo(VideoResolution.ORIGINAL))
    }

    @Test
    fun `parse dimensions test`() {
        verifyDimensions("1280x720", 1280, 720)
        verifyDimensions("60x53", 60, 53)
        assertThat(VideoResolution.parseDimensions("nosense"), nullValue())
        assertThat(VideoResolution.parseDimensions("adaptive"), nullValue())
        assertThat(VideoResolution.parseDimensions("original"), nullValue())
    }

    private fun verifyDimensions(resolution: String, width: Int, height: Int) {
        val pair = VideoResolution.parseDimensions(resolution)
        assertThat(pair!!.first, equalTo<Int>(width))
        assertThat(pair.second, equalTo<Int>(height))
    }
}
