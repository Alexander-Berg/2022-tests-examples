package ru.yandex.disk.download

import org.hamcrest.Matchers.equalTo
import org.junit.Test
import ru.yandex.disk.test.AndroidTestCase2
import ru.yandex.disk.test.SeclusiveContext
import ru.yandex.disk.test.TestObjectsFactory
import ru.yandex.util.Path

class DownloadQueueDatabaseTest : AndroidTestCase2() {

    private lateinit var downloadQueue: DownloadQueue
    private lateinit var downloadQueueDatabase: DownloadQueueDatabase
    private lateinit var context: SeclusiveContext

    private val path1 = Path("/disk/1")
    private val path2 = Path("/disk/2")

    override fun setUp() {
        super.setUp()
        context = SeclusiveContext(mContext)
        downloadQueueDatabase = TestObjectsFactory.createDownloadQueueDatabase(context)
        downloadQueue = TestObjectsFactory.createDownloadQueue(downloadQueueDatabase)
    }

    override fun tearDown() {
        downloadQueueDatabase.dbOpener.close()
        super.tearDown()
    }

    @Test
    fun `should return count of items in empty download queue`() {
        assertThat("expect no items in queue",
            downloadQueueDatabase.downloadQueueSize, equalTo(0))
    }

    @Test
    fun `should return count of added items`() {
        addSyncDownloadQueueItems(path1, 650)
        addSyncDownloadQueueItems(path2, 300)

        assertThat("expect 950 items in queue",
            downloadQueueDatabase.downloadQueueSize, equalTo(950))

        removeSyncDownloadQueueItems(path1)
        removeSyncDownloadQueueItems(path2)
    }

    @Test
    fun `should return count of remaining items`() {
        addSyncDownloadQueueItems(path1, 650)
        addSyncDownloadQueueItems(path2, 300)
        removeSyncDownloadQueueItems(path1)

        assertThat("expect 300 items in queue",
            downloadQueueDatabase.downloadQueueSize, equalTo(300))

        removeSyncDownloadQueueItems(path2)
    }

    @Test
    fun `should return count of items in cleared download queue`() {
        addSyncDownloadQueueItems(path1, 650)
        addSyncDownloadQueueItems(path2, 300)
        removeSyncDownloadQueueItems(path1)
        removeSyncDownloadQueueItems(path2)

        assertThat("expect no items in queue",
            downloadQueueDatabase.downloadQueueSize, equalTo(0))
    }

    private fun addSyncDownloadQueueItems(path: Path, count: Int) {
        for (i in 0 until count) {
            downloadQueue.add(DownloadQueueItem.Type.SYNC, path, null, 0, 0)
        }
    }

    private fun removeSyncDownloadQueueItems(path: Path) {
        downloadQueue.removeSyncItemsByPath(path)
    }
}
