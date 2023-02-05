package ru.yandex.disk.notifications

import org.junit.Test
import org.robolectric.annotation.Config
import ru.yandex.disk.test.AndroidTestCase2

class NotificationPresenterTest : AndroidTestCase2() {
    private val validLinks = arrayOf("http://www.foo.com", "https://www.foo.com",
            "http://foo.com", "https://foo.com"
            //TODO @delphin look at this "www.foo.com", "foo.com", "foo.foo.com"
    )
    private val invalidLinks = arrayOf(null, "", " ", "a", "1", "a.", "1.")

    @Test
    fun `should links be valid`() {
        for (link in validLinks) {
            assertTrue("link $link", BaseNotificationBuilderFactory.isLinkValid(link))
        }
    }

    @Test
    fun `should links be invalid`() {
        for (link in invalidLinks) {
            assertFalse(BaseNotificationBuilderFactory.isLinkValid(link))
        }
    }
}
