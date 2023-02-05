package ru.yandex.disk.trash

import com.google.common.collect.Iterables
import com.google.common.collect.Lists
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.not
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import ru.yandex.disk.event.DiskEvents
import ru.yandex.disk.event.EventLogger
import ru.yandex.disk.fetchfilelist.AnyDiffDetector
import ru.yandex.disk.sync.SyncItem
import ru.yandex.disk.test.Assert2.assertThat
import ru.yandex.disk.test.TestObjectsFactory
import java.util.Arrays.asList

@RunWith(RobolectricTestRunner::class)
class TrashDatabaseSyncerTest {

    private val eventLogger = EventLogger()
    private var database = TestObjectsFactory.createTrashDatabase(
            TrashDatabaseOpenHelper(RuntimeEnvironment.application, "name", 1),
            eventLogger)
    private var syncer = TrashDatabaseSyncer(database)

    @Test
    fun shouldAddNewItems() {
        val fileA = TrashItem.Builder().setPath("trash:/a").build()
        val fileB = TrashItem.Builder().setPath("trash:/b").build()

        emulateSync(fileA, fileB)

        assertThat(queryAllPaths(), equalTo(asList("trash:/a", "trash:/b")))
    }

    @Test
    fun shouldDeleteMissedItems() {
        val fileA = TrashItem.Builder().setPath("trash:/a").build()
        database.updateOrInsert(TrashItemRow(fileA))

        val fileB = TrashItem.Builder().setPath("trash:/b").build()

        emulateSync(fileB)

        assertThat(queryAllPaths(), equalTo(asList("trash:/b")))
    }

    @Test
    fun shouldFileChangedToDirectory() {
        val fileA = TrashItem.Builder().setPath("trash:/a").build()
        database.updateOrInsert(TrashItemRow(fileA))

        val dirA = TrashItem.Builder().setPath("trash:/a").setDir(true).build()

        emulateSync(dirA)

        assertThat(queryAllPaths(), equalTo(asList("trash:/a")))
        assertThat(queryAll()[0].isDir, equalTo(true))
    }

    @Test
    fun shouldDetectFileContentChange() {
        val diffDetector = AnyDiffDetector<SyncItem>()
        syncer.addSyncListener(diffDetector)

        val oldFile = TrashItem.Builder().setPath("trash:/a").setETag("OLD").build()
        database.updateOrInsert(TrashItemRow(oldFile))

        val newFile = TrashItem.Builder().setPath("trash:/a").setETag("NEW").build()

        emulateSync(newFile)

        assertThat(queryAllPaths(), equalTo(asList("trash:/a")))
        assertThat(queryAll()[0].eTag, equalTo("NEW"))
        assertThat(diffDetector.isChangeDetected, equalTo(true))
    }

    @Test
    fun shouldPutAllFieldToDatabase() {
        val remote = TrashItem.Builder()
                .setPath("trash:/a")
                .setDir(false)
                .setETag("ETAG")
                .setDisplayName("a")
                .setSize(1024)
                .setDeleted(10241024)
                .setMimeType("mime/type")
                .setMediaType("image")
                .setHasThumbnail(true)
                .build()

        emulateSync(remote)

        assertThat(queryAllPaths(), equalTo(asList("trash:/a")))
        val local = queryAll()[0]
        assertThat(local.path, equalTo("trash:/a"))
        assertThat(local.isDir, equalTo(false))
        assertThat(local.eTag, equalTo("ETAG"))
        assertThat(local.displayName, equalTo("a"))
        assertThat(local.size, equalTo(1024L))
        assertThat(local.lastModified, equalTo(10241024L))
        assertThat(local.mimeType, equalTo("mime/type"))
        assertThat(local.mediaType, equalTo("image"))
        assertThat(local.hasThumbnail, equalTo(true))
    }

    @Test
    fun shouldFlushAfter100Elements() {
        val items = arrayOfNulls<TrashItem>(200)
        val builder = TrashItem.Builder()
        for (i in items.indices) {
            items[i] = builder
                    .setPath("trash:/" + i)
                    .setDir(false)
                    .setETag("ETAG")
                    .setDisplayName("a")
                    .setSize(1024)
                    .setDeleted(10241024)
                    .setMimeType("mime/type")
                    .setMediaType("image")
                    .setHasThumbnail(true)
                    .build()

        }
        emulateSync(*items)

        assertThat(queryAllPaths().size, equalTo(200))
    }

    @Test
    fun shouldNotifyAboutItemDeleting() {
        val fileA = TrashItem.Builder().setPath("trash:/a").build()
        database.updateOrInsert(TrashItemRow(fileA))

        emulateSync()

        val trashChangedEvents = eventLogger.findAllByClass(DiskEvents.TrashListChanged::class.java)
        assertThat(trashChangedEvents, not(emptyList()))
    }

    private fun queryAllPaths(): List<String> {
        return Lists.transform(queryAll()) { input -> input!!.path }
    }

    private fun queryAll(): List<TrashItem> {
        val all = database.queryAll()
        val out = Lists.newArrayList(Iterables.transform(all
        ) { input -> TrashItem.Builder(input).build() })
        all.close()
        return out
    }

    private fun emulateSync(vararg files: TrashItem?) {
        syncer.begin()
        for (file in files) {
            syncer.collect(file!!)
        }
        syncer.commit()
        syncer.finish()
    }
}
