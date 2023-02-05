package ru.yandex.disk.provider

import org.hamcrest.Matchers.equalTo
import org.junit.Test
import ru.yandex.disk.provider.FileTree.directory
import ru.yandex.disk.provider.FileTree.file
import java.util.Arrays.asList

class ContainsAnyMethodTest : DiskDatabaseMethodTest() {

    @Test
    fun `should return true if contains`() {
        val filename = "1999" //the last file in chekced list
        FileTree.create().content(
                directory("A").content(
                        directory("B").content(file(filename))
                )
        ).insertToDiskDatabase(diskDb)

        val files = Array(2000, { i -> i.toString() }).toList()
        assertThat(diskDb.containsAny("/disk/A/B", files), equalTo(true))
    }

    @Test
    fun `should escape names`() {
        diskDb.containsAny("/disk", asList("'"))
    }

}