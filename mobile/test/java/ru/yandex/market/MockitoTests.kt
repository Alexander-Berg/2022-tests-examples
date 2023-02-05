package ru.yandex.market

import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.junit.Test

class MockitoTests {

    @Test
    fun `inOrder correctly remembers all arguments`() {
        val mock = mock<(String) -> Unit>()
        mock.invoke("one")
        mock.invoke("two")

        mock.inOrder {
            verify().invoke("one")
            verify().invoke("two")
        }
    }
}