package ru.yandex.disk.gallery.utils

import org.junit.Test
import ru.yandex.disk.gallery.data.model.MediaItem
import ru.yandex.disk.gallery.data.model.Section
import kotlin.test.assertNotEquals

class GalleryItemViewIdGeneratorTest {
    @Test
    fun `should be different view ids`() {
        val id = System.currentTimeMillis()

        val section = Section(id, 0, 0, 0, 0, 0, 0)
        val localMediaItem = MediaItem(id, null, "", 0, null, null, 0, null, null, 0.0, 0.0)
        val serverMediaItem = MediaItem(null, id, "", 0, 0, null, 0, null, null, 0.0, 0.0)

        val sectionViewId = GalleryItemViewIdGenerator.generateSectionViewId(section)
        val localMediaItemViewId = GalleryItemViewIdGenerator.generateMediaItemViewId(localMediaItem)
        val serverMediaItemViewId = GalleryItemViewIdGenerator.generateMediaItemViewId(serverMediaItem)

        assertNotEquals(sectionViewId, localMediaItemViewId)
        assertNotEquals(localMediaItemViewId, serverMediaItemViewId)
        assertNotEquals(sectionViewId, serverMediaItemViewId)
    }
}
