package ru.yandex.disk.video

import android.telephony.TelephonyManager
import android.util.DisplayMetrics
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Test
import ru.yandex.disk.connectivity.NetworkState
import ru.yandex.disk.test.Assert2.assertThat
import ru.yandex.disk.utils.ScreenSize
import kotlin.NoSuchElementException

private const val ADAPTIVE = "adaptive"
private val MINIMUM_AND_MAXIMUM = setOf("240p", "1080p")
private val HD_AND_FULL_HD = setOf("720p", "1080p")
private val BIG_RESOLUTIONS = setOf("480p", "720p", "1080p")
private val ADAPTIVE_AND_HD_RESOLUTIONS = HD_AND_FULL_HD + ADAPTIVE
private val ADAPTIVE_AND_BIG_RESOLUTIONS = BIG_RESOLUTIONS + ADAPTIVE
private val ORDINARY_RESOLUTIONS = HD_AND_FULL_HD + setOf("240p", "360p", "480p")
private val ADAPTIVE_AND_ORDINARY_RESOLUTIONS = ORDINARY_RESOLUTIONS + ADAPTIVE

class DefaultVideoResolutionPolicyTest {

    private val networkState = mock<NetworkState> {
        on { isWifiActiveNetwork } doReturn true
    }
    private val telephonyManager = mock<TelephonyManager>()
    private var screenSize = ScreenSize(1080, 1920)
    private val policy by lazy { DefaultVideoResolutionPolicy(screenSize, telephonyManager, networkState) }

    @Test
    fun `should use the only possible resolution`() {
        val result = policy.getDefaultResolutionFromSet(setOf("240p"))

        assertThat(result, equalTo(VideoResolution.p240))
    }

    @Test
    fun `should use adaptive resolution if possible`() {
        val result = policy.getDefaultResolutionFromSet(ADAPTIVE_AND_ORDINARY_RESOLUTIONS)

        assertThat(result, equalTo(VideoResolution.ADAPTIVE))
    }

    @Test
    fun `should use hd resolution on wi fi`() {

        val result = policy.getDefaultResolutionFromSet(ORDINARY_RESOLUTIONS)

        assertThat(result, equalTo(VideoResolution.p720))
    }

    @Test
    fun `should use hd resolution on lte`() {
        whenever(networkState.isWifiActiveNetwork).thenReturn(false)
        whenever(telephonyManager.networkType).thenReturn(TelephonyManager.NETWORK_TYPE_LTE)

        val result = policy.getDefaultResolutionFromSet(ORDINARY_RESOLUTIONS)

        assertThat(result, equalTo(VideoResolution.p720))
    }

    @Test
    fun `should use 480p resolution on other networks`() {
        whenever(networkState.isWifiActiveNetwork).thenReturn(false)
        whenever(telephonyManager.networkType).thenReturn(TelephonyManager.NETWORK_TYPE_HSDPA)

        val result = policy.getDefaultResolutionFromSet(ORDINARY_RESOLUTIONS)

        assertThat(result, equalTo(VideoResolution.p480))
    }

    @Test
    fun `should use 480p if screen width is 480 pixels`() {
        setScreenSize(480)

        val result = policy.getDefaultResolutionFromSet(ADAPTIVE_AND_ORDINARY_RESOLUTIONS)

        assertThat(result, equalTo(VideoResolution.p480))
    }

    @Test
    fun `should use 360p if screen width is smaller than 480 pixels`() {
        setScreenSize(479)

        val result = policy.getDefaultResolutionFromSet(ADAPTIVE_AND_ORDINARY_RESOLUTIONS)

        assertThat(result, equalTo(VideoResolution.p360))
    }

    @Test
    fun `should use 360p if screen width is smaller than 360 pixels`() {
        setScreenSize(360)

        val result = policy.getDefaultResolutionFromSet(ADAPTIVE_AND_ORDINARY_RESOLUTIONS)

        assertThat(result, equalTo(VideoResolution.p360))
    }

    @Test
    fun `should use 240p if screen width is smaller than 360 pixels`() {
        setScreenSize(359)

        val result = policy.getDefaultResolutionFromSet(ADAPTIVE_AND_ORDINARY_RESOLUTIONS)

        assertThat(result, equalTo(VideoResolution.p240))
    }

    @Test
    fun `should use 720p if smaller size is not available`() {
        whenever(networkState.isWifiActiveNetwork).thenReturn(false)

        val result = policy.getDefaultResolutionFromSet(HD_AND_FULL_HD)

        assertThat(result, equalTo(VideoResolution.p720))
    }

    @Test
    fun `should use smallest resolution on small screen if not available small resolution`() {
        setScreenSize(360)

        val result = policy.getDefaultResolutionFromSet(ADAPTIVE_AND_BIG_RESOLUTIONS)

        assertThat(result, equalTo(VideoResolution.p480))
    }

    @Test
    fun `should use adaptive resolution on small screen instead of HD`() {
        setScreenSize(360)

        val result = policy.getDefaultResolutionFromSet(ADAPTIVE_AND_HD_RESOLUTIONS)

        assertThat(result, equalTo(VideoResolution.ADAPTIVE))
    }

    @Test
    fun `should use 1080p instead 240p on normal screens`() {
        val result = policy.getDefaultResolutionFromSet(MINIMUM_AND_MAXIMUM)

        assertThat(result, equalTo(VideoResolution.p1080))
    }

    @Test
    fun `should use 240p instead 1080p on small screens`() {
        setScreenSize(480)

        val result = policy.getDefaultResolutionFromSet(MINIMUM_AND_MAXIMUM)

        assertThat(result, equalTo(VideoResolution.p240))
    }

    @Test(expected = NoSuchElementException::class)
    fun `should not use unknown resolution`() {
        policy.getDefaultResolutionFromSet(setOf("1080i"))
    }

    @Test
    fun `should use 360p on 240 pixel screen if it smallest available resolution`() {
        setScreenSize(240)
        val result = policy.getDefaultResolutionFromSet(ADAPTIVE_AND_ORDINARY_RESOLUTIONS - "240p")

        assertThat(result, equalTo(VideoResolution.p360))
    }

    private fun setScreenSize(size: Int) {
        screenSize = ScreenSize(size, size * 2)
    }
}
