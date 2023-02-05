package ru.yandex.disk.gallery.data.provider

import android.database.Cursor
import android.database.MatrixCursor
import android.provider.MediaStore
import org.hamcrest.Matchers.equalTo
import org.junit.Test
import ru.yandex.disk.test.AndroidTestCase2

class MediaStoreCursorsTest : AndroidTestCase2() {
    @Test
    fun `should return image mime type`() {
        val item = MediaStoreImageCursor(createCursor(mimeType = "image/jpeg")).singleAndCopy()!!
        assertThat(item.mimeType, equalTo("image/jpeg"))
    }

    @Test
    fun `should return image mime type from path`() {
        val item = MediaStoreImageCursor(createCursor(path = "storage/emulated/0/folder/file.jpeg")).singleAndCopy()!!
        assertThat(item.mimeType, equalTo("image/jpeg"))
    }

    @Test
    fun `should return default image mime type`() {
        val item = MediaStoreImageCursor(createCursor()).singleAndCopy()!!
        assertThat(item.mimeType, equalTo("image/*"))
    }

    private fun createCursor(path: String = "filepath", mimeType: String? = null): Cursor {
        return MatrixCursor(arrayOf(
                MediaStore.Images.ImageColumns._ID,
                MediaStore.Images.ImageColumns.DATA,
                MediaStore.Images.ImageColumns.BUCKET_ID,
                MediaStore.Images.ImageColumns.DATE_TAKEN,
                MediaStore.MediaColumns.DATE_ADDED,
                MediaStore.Images.ImageColumns.DATE_MODIFIED,
                MediaStore.Images.ImageColumns.SIZE,
                MediaStore.Images.ImageColumns.MIME_TYPE,
                MediaStore.Images.ImageColumns.WIDTH,
                MediaStore.Images.ImageColumns.HEIGHT
        )).apply {
            addRow(arrayOf(
                    0,
                    path,
                    "bucket_id",
                    0,
                    0,
                    0,
                    0,
                    mimeType,
                    0,
                    0
            ))
        }
    }
}