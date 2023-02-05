package ru.yandex.disk.util

import org.hamcrest.CoreMatchers.equalTo
import org.junit.Test
import org.robolectric.annotation.Config
import ru.yandex.disk.utils_ui.R
import ru.yandex.disk.test.AndroidTestCase2

@Config(manifest = Config.NONE)
class FileTypeIconsTest : AndroidTestCase2() {
    init {
        FileTypeIcons.init()
    }

    @Test
    fun `should find type by extension`() {
        val icons = FileTypeIcons.getByExtension("7z")
        assertThat(icons.iconResId, equalTo(R.drawable.ic_filetype_icon_archive_7z))
    }

    @Test
    fun `should return unknown type if unknown extension`() {
        val icons = FileTypeIcons.getByExtension("file_ext")
        assertThat(icons.iconResId, equalTo(R.drawable.ic_filetype_icon_unknown))
    }

    @Test
    fun `should find type by media type and extension`() {
        val icons = FileTypeIcons.getByMediaTypeAndExtension("image", "img")
        assertThat(icons.iconResId, equalTo(R.drawable.ic_filetype_icon_img))
    }

    @Test
    fun `should return unknown type if unknown media type and extension`() {
        val icons = FileTypeIcons.getByMediaTypeAndExtension("new_media_type", "file_ext")
        assertThat(icons.iconResId, equalTo(R.drawable.ic_filetype_icon_unknown))
    }
}