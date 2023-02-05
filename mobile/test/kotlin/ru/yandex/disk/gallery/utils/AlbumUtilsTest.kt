package ru.yandex.disk.gallery.utils

import org.hamcrest.collection.IsIterableContainingInOrder.contains
import org.junit.Test
import ru.yandex.disk.domain.albums.BucketAlbum
import ru.yandex.disk.domain.albums.BucketAlbumId
import ru.yandex.disk.test.Assert2.assertThat
import ru.yandex.disk.util.AlbumUtils

class AlbumUtilsTest {
    private val albumCamera = createAlbum("camera", "camera")
    private val albumDCIM = createAlbum("dcim", "DcIm")
    private val albumScreenshots = createAlbum("screenshots", "Screenshots")
    private val albumTelegram = createAlbum("telegram", "Telegram X")
    private val albumWhatsup = createAlbum("whatsapp", "WhatsApp")

    private val anotherAlbum1 = createAlbum("another_1", "1")
    private val anotherAlbum2 = createAlbum("another_2", "Фотокамера")
    private val anotherAlbum3 = createAlbum("another_3", "вертикальные панорамы")

    @Test
    fun `should sort albums by name`() {
        val sorted = mutableListOf(anotherAlbum1, anotherAlbum2, anotherAlbum3,
                albumTelegram, albumScreenshots, albumCamera, albumDCIM, albumWhatsup).apply {
            sortWith(Comparator { a1, a2 -> AlbumUtils.compare(a1, a2) })
        }
        assertThat(sorted, contains(albumCamera, albumDCIM, albumScreenshots, albumTelegram,
                albumWhatsup, anotherAlbum1, anotherAlbum3, anotherAlbum2))
    }

    private fun createAlbum(bucketId: String, name: String): BucketAlbum {
        return BucketAlbum(BucketAlbumId(bucketId), name, 0, 0, BucketAlbum.AutoUploadMode.DISABLED,
            "", 0, false, false, true)
    }
}
