package ru.yandex.disk

import android.content.pm.PackageManager
import android.content.res.Resources
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.junit.Test
import org.mockito.Mockito.*

private const val TEST_DEVICE_ID = "test device id"
private const val TEST_UUID = "testUUID"

class DiskUserAgentProviderTest {

    private val startupData = mock<StartupData> {
        on { deviceId } doReturn TEST_DEVICE_ID
        on { uuid } doReturn TEST_UUID
    }
    private val resources = mock<Resources>()
    private val packageManager = mock<PackageManager>()

    private val provider = DiskUserAgentProvider(startupData, resources, packageManager)

    @Test
    fun `should return UserAgent with device id`() {
        val userAgent = provider.userAgent
        assertThat(userAgent, containsString(TEST_DEVICE_ID))
    }

    @Test
    fun `should cache good device id`() {
        provider.userAgent // first call
        provider.userAgent // second call

        verify(startupData).deviceId
        verify(startupData).uuid
        verifyNoMoreInteractions(startupData)
    }

}
