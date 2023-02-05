package ru.yandex.market.ui.view.contact

import org.junit.Assert.fail
import org.junit.Test

class ContactTest {

    @Test
    fun `Empty factory method not throw exceptions`() {
        try {
            Contact.empty()
        } catch (e: Throwable) {
            fail("Contact.empty() throw exception $e")
        }
    }
}