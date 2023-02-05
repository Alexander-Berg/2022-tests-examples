package ru.yandex.disk.gallery.data.sync

import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.nullValue
import org.junit.Before
import org.junit.Test
import ru.yandex.disk.gallery.data.GalleryDataTestCase
import ru.yandex.disk.gallery.data.database.MediaItemModel
import ru.yandex.disk.gallery.data.model.SyncStatuses
import ru.yandex.disk.photoslice.MomentItemMapping
import ru.yandex.disk.provider.DiskItemBuilder
import ru.yandex.disk.upload.FilesTimeExtractor
import ru.yandex.disk.util.MediaTypes
import ru.yandex.util.Path
import java.util.concurrent.TimeUnit

class PhotosliceMergeHandlerTest : GalleryDataTestCase() {

    private lateinit var handler: PhotosliceMergeHandler

    @Before
    override fun setUp() {
        super.setUp()

        handler = photosliceMerger
    }

    @Test
    fun `should find by names`() {
        val matched = createMediaItem("/matched.ext", minutes(100), 100, null)
        val unmatched = createMediaItem("/unmatched.ext", minutes(200), 200, null)

        val moved = createMediaItem("/moved.ext", minutes(300), 300L, null)
        val missed = createMediaItem("/missed.ext", minutes(400), 400, null)

        insertMomentItemWithTimePath(matched.path, matched.eTime, matched.size, "e1")
        insertMomentItemWithTimePath(unmatched.path, unmatched.eTime, unmatched.size + 100, "e2")
        insertMomentItem("/moved" + timePath(moved.path, moved.eTime), moved.eTime, moved.size, "e3")

        val found = handler.findMatchingMomentItems(listOf(matched, unmatched, moved, missed))

        assertThat(found[0]?.eTag, equalTo("e1"))
        assertThat(found[1], nullValue())
        assertThat(found[2]?.eTag, equalTo("e3"))
        assertThat(found[3], nullValue())
    }

    @Test
    fun `should find by eTimes`() {
        val matched = createMediaItem("/matched.ext", minutes(100), 100, null)
        val unmatched = createMediaItem("/unmatched.ext", minutes(200), 200, null)

        val double = createMediaItem("/double.ext", matched.eTime, matched.size, null)
        val fallenBack = createMediaItem("/fallen.ext", minutes(300), 300, null)

        insertMomentItem("/path1", matched.eTime, matched.size, "e1")
        insertMomentItem("/path2", unmatched.eTime, unmatched.size + 100, "e2")

        insertMomentItemWithTimePath(fallenBack.path, fallenBack.eTime, fallenBack.size + 100, "e3")
        insertMomentItem("/path3", fallenBack.eTime, fallenBack.size, "e4")

        val found = handler.findMatchingMomentItems(listOf(matched, unmatched, double, fallenBack))

        assertThat(found[0]?.eTag, equalTo("e1"))
        assertThat(found[1], nullValue())
        assertThat(found[2]?.eTag, equalTo("e1"))
        assertThat(found[3]?.eTag, equalTo("e4"))
    }

    @Test
    fun `should find by floored eTimes`() {
        val exactly = createMediaItem("/exactly.ext", minutes(100), 100, null)
        val floored = createMediaItem("/floored.ext", minutes(100) + 999, 100, null)
        val unmatched = createMediaItem("/unmatched.ext", minutes(100) + 999, 200, null)

        insertMomentItem("/path1", minutes(100), exactly.size, "e1")

        val found = handler.findMatchingMomentItems(listOf(exactly, floored, unmatched))

        assertThat(found[0]?.eTag, equalTo("e1"))
        assertThat(found[1]?.eTag, equalTo("e1"))
        assertThat(found[2]?.eTag, nullValue())
    }

    @Test
    fun `should find by hashes`() {
        val single1 = createMediaItem("/single.ext", 0, 0, "single")

        val double1 = createMediaItem("/double1.ext", 0, 100, "double")
        val double2 = createMediaItem("/double2.ext", 0, 200, "double")

        val unmatched = createMediaItem("/missed.ext", minutes(100), 300, "missed")

        insertMomentItem("/path1", single1.eTime + minutes(10), single1.size, single1.md5!!)
        insertMomentItem("/path2", double2.eTime, double2.size, double1.md5!!)
        insertMomentItemWithTimePath(unmatched.path, unmatched.eTime, unmatched.size, "_")

        val found = handler.findMatchingMomentItems(listOf(single1, double1, double2, unmatched))

        assertThat(found[0]?.eTag, equalTo(single1.md5!!))
        assertThat(found[1]?.eTag, equalTo(double1.md5!!))
        assertThat(found[2]?.eTag, equalTo(double2.md5!!))
        assertThat(found[3], nullValue())
    }

    @Test
    fun `should find upload queue items`() {
        val matched = createMediaItem("/1", 0, 10)

        val duplicated = createMediaItem("/2", 0, 20)
        val duplicate = createMediaItem("/2", 0, 100500)

        val mismatched = createMediaItem("/3", 0, 20)

        insertAutouploadedQueueItem(matched.path, matched.size, 10, "/11")
        insertAutouploadedQueueItem(duplicated.path, duplicated.size, 20, md5 = "md5")
        insertAutouploadedQueueItem("/4", matched.size, 30)

        val found = handler.findMatchingUploadQueueItems(listOf(matched, duplicate, duplicated, mismatched))

        assertThat(found.map { it?.uploadTime }, equalTo(listOf(10L, null, 20L, null)))
        assertThat(found.map { it?.uploadPath }, equalTo(listOf("/11", null, null, null)))
        assertThat(found.map { it?.md5 }, equalTo(listOf(null, null, "md5", null)))
    }

    @Test
    fun `should keep local album items unresolved`() {
        val items = listOf(
                createMediaItem("/1", 0, photosliceTime = 1),
                createMediaItem("/2", 0, photosliceTime = null),
                createMediaItem("/3", 0, photosliceTime = 1),
                createMediaItem("/4", 0, photosliceTime = 1),
                createMediaItem("/5", 0, photosliceTime = null))

        val statuses = listOf(
                SyncStatuses.WONT_AUTOUPLOAD,
                SyncStatuses.UNRESOLVED,
                SyncStatuses.WONT_AUTOUPLOAD,
                SyncStatuses.WONT_AUTOUPLOAD,
                SyncStatuses.UNRESOLVED)

        assertThat(handler.resolveSyncData(items).map { it.status }, equalTo(statuses))
    }

    private fun createMediaItem(
            path: String, eTime: Long, size: Long = 0,
            md5: String? = null, photosliceTime: Long? = eTime): MediaItemModel {

        return consMediaItem(path = path, eTime = eTime, size = size, md5 = md5, photosliceTime = photosliceTime)
    }

    private fun insertMomentItemWithTimePath(path: String, eTime: Long, size: Long, eTag: String) {
        insertMomentItem(timePath(path, eTime), eTime, size, eTag)
    }

    private fun insertMomentItem(path: String, eTime: Long, size: Long, eTag: String) {
        val itemId = listOf(path, eTag, eTime, size).joinToString("_") { it.toString() }

        momentsDatabase.insertOrReplace("momentId", MomentItemMapping(itemId, path))

        insertDiskItem(DiskItemBuilder().setMediaType(MediaTypes.IMAGE)
                .setPath(path).setEtag(eTag).setSize(size).build(), eTime)
    }

    private fun timePath(path: String, eTime: Long): String {
        val parsed = Path.asPath(path)!!
        val parent = parsed.parentPath!!

        0 until 2

        return parent + ("/".takeIf { !parent.endsWith("/") } ?: "") +
                FilesTimeExtractor.fileTimestampName(parsed.name, eTime)
    }

    private fun minutes(n: Int) = TimeUnit.MINUTES.toMillis(n.toLong())
}
