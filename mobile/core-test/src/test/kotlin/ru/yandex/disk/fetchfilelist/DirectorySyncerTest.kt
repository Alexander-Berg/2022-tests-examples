package ru.yandex.disk.fetchfilelist

import org.mockito.kotlin.any
import org.hamcrest.Matchers.equalTo
import org.junit.Test
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import ru.yandex.disk.DiskItem
import ru.yandex.disk.FileItem.OfflineMark
import ru.yandex.disk.provider.DiskFileCursor
import ru.yandex.disk.provider.DiskItemBuilder
import ru.yandex.disk.provider.FileTree
import ru.yandex.disk.provider.FileTree.directory
import ru.yandex.disk.provider.FileTree.file
import ru.yandex.util.Path.asPath

abstract class DirectorySyncerTest : SyncListenerTest() {

    protected var selection: DiskFileCursor? = null
    protected var directory: DiskItem? = null

    protected fun createSyncer(dirToList: String? = null): DirectorySyncer<*>? {
        if (dirToList != null) {
            directory = diskDatabase.queryDirectory(asPath(dirToList)!!)
            return createDirectorySyncer()
        } else {
            return null
        }
    }

    protected abstract fun createDirectorySyncer(): DirectorySyncer<*>
    
    override fun tearDown() {
        selection?.close()
        super.tearDown()
    }

    @Test
    fun testShouldProcessRootDirectory() {
        val fileFromServer = DiskItemBuilder().setPath("/disk/A").build()

        syncer = createSyncer("/disk")

        emulateSync(fileFromServer)

        val fileInDb = diskDatabase.queryFileItem(asPath("/disk/A")!!)

        assertThat(fileInDb!!.offline, equalTo(OfflineMark.NOT_MARKED))
    }

    @Test
    fun testOfflineDirectoryShouldStayAs_MARKED_OFFLINE() {
        FileTree.create().content(directory("A").setOffline(OfflineMark.MARKED)).insertToDiskDatabase(diskDatabase)

        val fileA = DiskItemBuilder().setPath("/disk/A").setIsDir(true).build()

        syncer = createSyncer("/disk/A")

        emulateSync(fileA)

        val fileInDb = diskDatabase.queryFileItem(asPath("/disk/A")!!)

        assertThat(fileInDb!!.offline, equalTo(OfflineMark.MARKED))
    }

    @Test
    fun testFileInOfflienDirectoryShouldBeInsertAs_IN_OFFLINE_DIR() {
        FileTree.create().content(directory("A").setOffline(OfflineMark.MARKED)).insertToDiskDatabase(diskDatabase)

        val fileA = DiskItemBuilder().setPath("/disk/A").setIsDir(true).build()
        val fileAa = DiskItemBuilder().setPath("/disk/A/a").build()

        syncer = createSyncer("/disk/A")

        emulateSync(fileA, fileAa)

        val fileInDb = diskDatabase.queryFileItem(asPath("/disk/A/a")!!)

        assertThat(fileInDb!!.offline, equalTo(OfflineMark.IN_OFFLINE_DIRECTORY))
    }

    @Test
    fun testFileShouldBeRegularInRegularDirectory() {
        FileTree.create().content(directory("A")).insertToDiskDatabase(diskDatabase)

        val fileA = DiskItemBuilder().setPath("/disk/A").setIsDir(true).build()
        val fileAa = DiskItemBuilder().setPath("/disk/A/a").build()

        syncer = createSyncer("/disk/A")

        emulateSync(fileA, fileAa)

        val fileInDb = diskDatabase.queryFileItem(asPath("/disk/A")!!)
        assertThat(fileInDb!!.offline, equalTo(OfflineMark.NOT_MARKED))
    }

    @Test
    fun shouldNotifyAboutItemDelete() {
        FileTree.create().content(file("A")).insertToDiskDatabase(diskDatabase)
        syncer = createSyncer("/disk")

        emulateSyncEmptyList()

        verify(diskDatabase, times(1)).notifyChange(any())
    }
}
