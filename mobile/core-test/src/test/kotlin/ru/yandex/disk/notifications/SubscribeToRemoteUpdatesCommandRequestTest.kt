package ru.yandex.disk.notifications

import android.os.Bundle
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Test
import org.robolectric.annotation.Config
import ru.yandex.disk.remote.MessagingCloud
import ru.yandex.disk.test.AndroidTestCase2

private const val TEST_TOKEN = "test_token"

@Config(manifest = Config.NONE)
class SubscribeToRemoteUpdatesCommandRequestTest : AndroidTestCase2() {

    @Test
    fun `should restore from bundle properly`() {
        val request = SubscribeToRemoteUpdatesCommandRequest(TEST_TOKEN, MessagingCloud.GCM)
        val bundle = Bundle()
        request.save(bundle)

        val restoredRequest = SubscribeToRemoteUpdatesCommandRequest()
        restoredRequest.restore(bundle)

        assertThat(restoredRequest.getRegistrationId(), equalTo(TEST_TOKEN))
    }

}
