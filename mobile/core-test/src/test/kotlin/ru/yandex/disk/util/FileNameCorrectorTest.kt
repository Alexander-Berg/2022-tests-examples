package ru.yandex.disk.util


import org.junit.Test
import ru.yandex.disk.test.TestCase2

class FileNameCorrectorTest : TestCase2() {
    @Test
    @Throws(Exception::class)
    fun `calculated new names should be the same as expected`() {
        val directoryFileNames = listOf(
                "File name.jpg",
                "File name(1).jpg",
                "File name(2).jpg",
                "File name #1.jpg",
                "File (1) name #2.jpg",
                "File name #2.jpg",
                "File name #2.jpg (2)",
                "File name #3.jpg",
                "File name #3.(1)jpg",
                "File name #3.jp(1)g",
                "File",
                "File(1)",
                "File wo extension",
                "File wo extension#2(1)",
                "File name (1).jpg",
                "File name #4.jpg",
                "File name #4 (1).jpg"
        )

        val uploadingFileNames = listOf(
                "File name.jpg",
                "File name #1.jpg",
                "File name #2.jpg",
                "File name #3.jpg",
                "File",
                "File wo extension",
                "File wo extension#2(1)",
                "File name (1).jpg",
                "File name #4 (1).jpg"
        )

        val expectedFileNames = listOf(
                "File name(3).jpg",
                "File name #1(1).jpg",
                "File name #2(1).jpg",
                "File name #3(1).jpg",
                "File(2)",
                "File wo extension(1)",
                "File wo extension#2(2)",
                "File name (2).jpg",
                "File name #4 (2).jpg"
        )

        val correctedFileNames = FileNameCorrector
                .correct(directoryFileNames, uploadingFileNames)

        assertEquals(expectedFileNames.size, correctedFileNames.size)

        for ((index, item) in correctedFileNames.withIndex()) {
            assertEquals(expectedFileNames[index], item.second)
        }
    }
}
