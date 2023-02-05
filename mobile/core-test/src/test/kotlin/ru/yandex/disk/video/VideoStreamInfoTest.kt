package ru.yandex.disk.video

import org.mockito.kotlin.mock
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.mock
import ru.yandex.disk.remote.VideoUrlsApi

class VideoStreamInfoTest {

    @Test
    fun `should return error if empty urls list`() {
        val response = mock(VideoUrlsApi.VideoUrlsResponse::class.java)

        val info = VideoStreamInfo.make(response, mock(DefaultVideoResolutionPolicy::class.java))
        assertThat(info.hasError(), equalTo(true))
    }

    @Test
    fun `should make info`() {
        val resolution = VideoResolution.p720
        val response = createMockResponse(resolution)
        val defaultVideoResolutionPolicy = createMockDefaultResolutionPolicy(resolution)

        val info = VideoStreamInfo.make(response, defaultVideoResolutionPolicy)

        assertThat(info.getStreamUrl(resolution), equalTo("https://some/url"))
    }

    @Test
    fun `should return error if resolution is unsupported`() {
        val response = createMockResponse(VideoResolution.p720)
        val defaultVideoResolutionPolicy = createMockDefaultResolutionPolicy(VideoResolution.p240)

        val info = VideoStreamInfo.make(response, defaultVideoResolutionPolicy)

        assertThat(info.hasError(), equalTo(true))
    }

    private fun createMockDefaultResolutionPolicy(defaultResolution : VideoResolution) : DefaultVideoResolutionPolicy{
        return mock {
            on { getDefaultResolutionFromSet(ArgumentMatchers.anySet()) }.thenReturn(defaultResolution)
        }
    }

    private fun createMockResponse(videoResolution : VideoResolution) : VideoUrlsApi.VideoUrlsResponse {
        val resolution = mock<VideoUrlsApi.VideoUrlItem> {
            on { resolution }.thenReturn(videoResolution.resolution)
            on { url }.thenReturn("https://some/url")
        }
        return mock {
            on { urls }.thenReturn(listOf(resolution))
        }
    }
}
