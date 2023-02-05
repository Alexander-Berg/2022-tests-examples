package ru.yandex.disk.util

import org.hamcrest.Matchers.`is`
import org.junit.Assert.assertThat
import org.junit.Test

class NameWithExtTest {
    @Test
    fun `should correctly parse filenames without extension`() {
        assertThat(
            NameWithExt.parse("filename"),
            `is`(NameWithExt("filename", ""))
        )
    }

    @Test
    fun `should correctly parse filenames with extension`() {
        assertThat(
            NameWithExt.parse("filename.ext"),
            `is`(NameWithExt("filename", "ext"))
        )
    }

    @Test
    fun `should correctly parse filenames with empty extension`() {
        assertThat(
            NameWithExt.parse("filename."),
            `is`(NameWithExt("filename", ""))
        )
    }

    @Test
    fun `should construct filenames with extension correctly`() {
        assertThat(
            NameWithExt("filename", "ext").getFilename("_1"),
            `is`("filename_1.ext")
        )

    }

    @Test
    fun `should construct filenames without extension correctly`() {
        assertThat(
            NameWithExt("filename", " ").getFilename("_1"),
            `is`("filename_1")
        )
    }
}