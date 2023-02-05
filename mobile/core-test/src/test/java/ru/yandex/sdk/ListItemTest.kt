package ru.yandex.sdk

import com.yandex.disk.client.ListItem
import org.hamcrest.CoreMatchers
import org.junit.Test
import ru.yandex.disk.test.TestCase2

class ListItemTest : TestCase2() {
    @Test
    @Throws(Exception::class)
    fun testAutoFillDisplayName() {
        val listItem = ListItem(fullPath = "parent/name")
        assertThat(listItem.displayName, CoreMatchers.equalTo("name"))
    }
}
