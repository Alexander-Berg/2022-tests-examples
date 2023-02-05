package ru.yandex.disk.remote.webdav

import org.mockito.kotlin.mock
import okhttp3.OkHttpClient
import okhttp3.Request
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.yandex.disk.Credentials
import ru.yandex.disk.commonactions.SingleWebdavClientPool
import ru.yandex.disk.toggle.SeparatedAutouploadToggle
import java.util.Arrays.asList

@RunWith(RobolectricTestRunner::class)
class WebdavClientTest {

    @Test
    fun `should decode right`() {
        val strings = WebdavClient.decodePaths(asList("/disk/a", "disk/b", "/disk/%26"))
        assertThat(strings[0], equalTo("/disk/a"))
        assertThat(strings[1], equalTo("/disk/b"))
        assertThat(strings[2], equalTo("/disk/&"))
    }

    @Test
    fun `should reset client if new creds used`() {
        val pool = WebdavClient.Pool(OkHttpClient(), {}, WebdavClient.WebdavConfig.DEFAULT_HOST,
            mock(), SeparatedAutouploadToggle(false), mock())

        val op = WebdavClient.Op.QUEUE
        val first = pool.getClient(createCreds("first"), op)
        val second = pool.getClient(createCreds("second"), op)

        assertThat(first, not(sameInstance(second)))
    }

    @Test
    fun `should add not ascii headers`() {
        val requestBuilder = Request.Builder()
        requestBuilder.url("https://yandex.com")

        WebdavClient.addEncodedHeader(requestBuilder, "header1", "value")
        WebdavClient.addEncodedHeader(requestBuilder, "header2", "乐视超级手机1")
        WebdavClient.addEncodedHeader(requestBuilder, "header3", "Навальный20!8")

        val request = requestBuilder.build()
        assertThat(request.header("header1"), equalTo("value"))
        assertThat(request.header("header2"), equalTo("%E4%B9%90%E8%A7%86%E8%B6%85%E7%BA%A7%E6%89%8B%E6%9C%BA1"))
        assertThat(request.header("header3"), equalTo("%D0%9D%D0%B0%D0%B2%D0%B0%D0%BB%D1%8C%D0%BD%D1%8B%D0%B920%218"))
    }

    private fun createCreds(name: String) = Credentials(name, 0)
}
