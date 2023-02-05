package ru.yandex.disk.offline

import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers.equalTo
import org.junit.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.Mockito.*
import ru.yandex.disk.AppStartSessionProvider
import ru.yandex.disk.ApplicationStorage
import ru.yandex.disk.Credentials
import ru.yandex.disk.DeveloperSettings
import ru.yandex.disk.DiskItem
import ru.yandex.disk.FileItem
import ru.yandex.disk.ServerDiskItem
import ru.yandex.disk.Storage
import ru.yandex.disk.commonactions.SingleWebdavClientPool
import ru.yandex.disk.download.DownloadQueue
import ru.yandex.disk.download.DownloadQueueItem
import ru.yandex.disk.event.DiskEvents
import ru.yandex.disk.event.EventSender
import ru.yandex.disk.fetchfilelist.PathLock
import ru.yandex.disk.fetchfilelist.RegularFileStorageCleaner
import ru.yandex.disk.fetchfilelist.SyncException
import ru.yandex.disk.mocks.CredentialsManagerWithUser
import ru.yandex.disk.provider.*
import ru.yandex.disk.provider.FileTree.file
import ru.yandex.disk.remote.RemoteRepo
import ru.yandex.disk.remote.RestApiClient
import ru.yandex.disk.remote.exceptions.PermanentException
import ru.yandex.disk.remote.exceptions.TemporaryException
import ru.yandex.disk.remote.webdav.WebdavClient
import ru.yandex.disk.service.CommandLogger
import ru.yandex.disk.test.AndroidTestCase2
import ru.yandex.disk.test.MoreMatchers.anyPath
import ru.yandex.disk.test.Reflector
import ru.yandex.disk.test.SeclusiveContext
import ru.yandex.disk.test.TestObjectsFactory
import ru.yandex.disk.toggle.SeparatedAutouploadToggle
import ru.yandex.disk.util.asProvider
import ru.yandex.util.Path
import java.util.*

class OfflineFilesSyncOperationTest : AndroidTestCase2() {

    private lateinit var context: SeclusiveContext
    private lateinit var webdavMock: WebdavClient
    private lateinit var diskDatabase: DiskDatabase
    private lateinit var remoteRepo: RemoteRepo
    private lateinit var applicationStorage: ApplicationStorage
    private lateinit var storage: Storage
    private lateinit var credentialsManagerWithUser: CredentialsManagerWithUser
    private lateinit var downloadQueue: DownloadQueue
    private lateinit var eventSender: EventSender
    private lateinit var contentChangedNotifier: ContentChangeNotifier

    private fun createFileItem(path: String, etag: String): DiskItem {
        return DiskItemBuilder()
                .setPath(path).setEtag(etag).setOffline(FileItem.OfflineMark.MARKED).build()
    }

    enum class FileNames {
        remfile_tagmatch,
        remfile_tagdiff,
        remdir_tagmatch,
        remdir_tagdiff,
        remnone_tagmatch,
        remnone_tagdiff
    }

    override fun setUp() {
        super.setUp()
        context = SeclusiveContext(mContext)
        credentialsManagerWithUser = CredentialsManagerWithUser("test")
        webdavMock = mock(WebdavClient::class.java)
        val user = mock(Credentials::class.java)
        remoteRepo = RemoteRepo(user, SingleWebdavClientPool(webdavMock), mock(RestApiClient::class.java),
            mock(DeveloperSettings::class.java), SeparatedAutouploadToggle(false), mock(AppStartSessionProvider::class.java))
        diskDatabase = TestObjectsFactory.createDiskDatabase(DH(context), ContentChangeNotifierStub(), null)
        applicationStorage = mock(ApplicationStorage::class.java)
        storage = mock(Storage::class.java)
        downloadQueue = TestObjectsFactory.createDownloadQueue(context)
        val dbOpenHelper = TestObjectsFactory.createSqlite(context)
        contentChangedNotifier = mock(ContentChangeNotifier::class.java)
        diskDatabase = TestObjectsFactory.createDiskDatabase(dbOpenHelper, contentChangedNotifier, PathLock())
        eventSender = mock(EventSender::class.java)
    }

    override fun tearDown() {
        context.shutdown()
        super.tearDown()
        Reflector.scrub(this)
    }

    @Test
    fun `test download queue state after sync`() {

        val remoteFiles = ArrayList<DiskItem>()
        val fileTreeItems = ArrayList<FileTree.Item<*>>()

        for (item in FileNames.values()) {
            val filename = item.name

            if (filename.contains("remfile") || filename.contains("remdir")) {
                val filepath = "/disk/" + filename
                val tag = if (filename.contains("tagmatch")) "tagmatch" else "remotetag"

                if (filename.contains("remfile")) {
                    remoteFiles.add(createFileItem(filepath, tag))
                } else {
                    remoteFiles.add(DiskItemBuilder().setPath(filepath).setIsDir(true).build())
                }
            }

            fileTreeItems.add(
                    file(filename).setOffline(FileItem.OfflineMark.MARKED)
                            .setEtag(if (filename.contains("tagmatch")) "tagmatch" else "localtag")
                            .setEtagLocal(if (filename.contains("tagmatch")) "tagmatch" else "localtag")
            )
        }

        insertToDiskDatabase(fileTreeItems)

        whenWebdavGetFileListThen(remoteFiles)

        val expectedRemoved = HashSet<String>()
        expectedRemoved.add("/disk/" + FileNames.remdir_tagmatch.name)
        expectedRemoved.add("/disk/" + FileNames.remdir_tagdiff.name)
        expectedRemoved.add("/disk/" + FileNames.remnone_tagmatch.name)
        expectedRemoved.add("/disk/" + FileNames.remnone_tagdiff.name)

        whenStorageFileExistsThen(true)
        val expectedAdded = HashSet<String>()
        expectedAdded.add("/disk/" + FileNames.remfile_tagdiff.name)

        val actuallyRemoved = HashSet<String>()
        val actuallyAdded = HashSet<String>()

        downloadQueue = spy(downloadQueue)
        doAnswer { invocation ->
            val args = invocation.arguments
            val source = args[1] as Path
            actuallyAdded.add(source.toString())
            invocation.callRealMethod()
        }.`when`<DownloadQueue>(downloadQueue).add(any<DownloadQueueItem.Type>(), anyPath(),
                nullable(Path::class.java), anyLong(), anyLong())

        doAnswer { invocation ->
            val path = invocation.arguments[0] as Path
            actuallyRemoved.add(path.toString())
            invocation.callRealMethod() as Boolean
        }.`when`<DownloadQueue>(downloadQueue).removeSyncItemsByPath(anyPath())

        executeOperation()

        assertThat(actuallyAdded, equalTo<Set<String>>(expectedAdded))
        assertThat(actuallyRemoved, equalTo<Set<String>>(expectedRemoved))
    }

    @Test
    fun `test file queued if etag local null`() {
        val remoteFile = DiskItemBuilder().setPath("/disk/a.txt").setEtag("E").build()
        whenWebdavGetFileListThen(remoteFile)

        val localFile = FileTree.file("a.txt").setEtagLocal(null).setOffline(FileItem.OfflineMark.MARKED)
        insertToContentProvider(localFile)

        executeOperation()

        assertNotNull(downloadQueue.peek())
    }

    @Test
    fun `test file queued on tags mismatch`() {
        val remoteFile = DiskItemBuilder().setPath("/disk/a.txt").setEtag("E").build()
        whenWebdavGetFileListThen(remoteFile)
        whenStorageFileExistsThen(true)

        val localFile = FileTree.file("a.txt").setEtag("E").setEtagLocal("L").setOffline(FileItem.OfflineMark.MARKED)
        insertToContentProvider(localFile)

        executeOperation()

        MatcherAssert.assertThat("DownloadQueue.isEmpty()", !downloadQueue.isEmpty)
    }

    @Test
    fun `test file should be queued to download if null etag local`() {
        val remoteFile = DiskItemBuilder().setPath("/disk/a.txt").setEtag("E").build()
        whenWebdavGetFileListThen(remoteFile)
        whenStorageFileExistsThen(true)

        val localFile = FileTree.file("a.txt").setEtag("E").setOffline(FileItem.OfflineMark.MARKED)
        insertToContentProvider(localFile)

        executeOperation()

        MatcherAssert.assertThat("DownloadQueue.isEmpty()", !downloadQueue.isEmpty)
    }

    @Test
    fun `test should queue download with new file size`() {
        val remoteFile = DiskItemBuilder()
                .setPath("/disk/a.txt")
                .setEtag("ETAG")
                .setSize(1024)
                .build()
        whenWebdavGetFileListThen(remoteFile)

        FileTree.create().content(
                file("a.txt").setEtagLocal("L").setSize(2048).setOffline(FileItem.OfflineMark.MARKED)
        ).insertToDiskDatabase(diskDatabase)

        executeOperation()

        val downloadTask = downloadQueue.peek()
        assertNotNull(downloadTask)
        MatcherAssert.assertThat(downloadTask.size, equalTo(1024L))

    }

    @Test
    fun `test file queued tags equals but cached file does not exist`() {
        val remoteFile = DiskItemBuilder().setPath("/disk/a.txt").setEtag("E").build()
        whenWebdavGetFileListThen(remoteFile)

        val localFile = FileTree.file("a.txt").setEtagLocal("E").setOffline(FileItem.OfflineMark.MARKED)
        insertToContentProvider(localFile)

        whenStorageFileExistsThen(false)

        executeOperation()

        assertNotNull(downloadQueue.peek())
    }

    @Test
    fun `test should delete cached file from storage if deleted on server`() {
        storage = mock(Storage::class.java)

        FileTree.create().content(
                file("a.txt").setEtagLocal("E").setOffline(FileItem.OfflineMark.MARKED)
        ).insertToDiskDatabase(diskDatabase)
        whenWebdavGetFileListThen()

        executeOperation()

        verify(storage).deleteFileOrFolder("/disk/a.txt")
    }

    @Test
    fun `test should not start sync if not offline files`() {
        executeOperation()

        verify(webdavMock, never()).getFileList(anyCollection())
    }

    @Test
    fun `should send remote dir changed event on file deleting`() {
        addOfflineFileToDatabase()

        executeOperation()

        verify(eventSender, times(1)).send(any<DiskEvents.RemoteDirectoryChanged>())
    }

    @Test
    fun `should not send notify to database on file deleting`() {
        addOfflineFileToDatabase()

        executeOperation()

        verify(contentChangedNotifier, never()).notifyChange(anyPath())
    }

    private fun addOfflineFileToDatabase() {
        FileTree.create()
                .content(file("a.txt").setEtagLocal("E").setOffline(FileItem.OfflineMark.MARKED))
                .insertToDiskDatabase(diskDatabase)
    }

    private fun whenStorageFileExistsThen(value: Boolean) {
        `when`(storage.fileExists(anyString())).thenReturn(value)
    }

    private fun insertToContentProvider(localFile: FileTree.Item<*>) {
        insertToDiskDatabase(listOf(localFile))
    }

    private fun whenWebdavGetFileListThen(remoteFiles: List<DiskItem>) {
        whenWebdavGetFileListThen(*remoteFiles.toTypedArray())
    }

    private fun anyStringList(): List<String> {
        return anyList()
    }

    private fun insertToDiskDatabase(fileTreeItems: List<FileTree.Item<*>>) {
        val tree = FileTree()
        tree.root().content(*fileTreeItems.toTypedArray())
        tree.insertToDiskDatabase(diskDatabase)
    }

    private fun whenWebdavGetFileListThen(vararg remoteFiles: DiskItem) {
        @Suppress("UNCHECKED_CAST")
        val result = Arrays.asList(*remoteFiles) as List<ServerDiskItem>

        `when`(webdavMock.getFileList(anyStringList())).thenReturn(result)
    }

    private fun createOperation(): OfflineFilesSyncOperation {
        val commandStarter = CommandLogger()
        val downloadEnqueuerFactory = object : OfflineDownloadEnqueuerFactory {
            override fun create(analyticsEventName: String): OfflineDownloadEnqueuer {
                return OfflineDownloadEnqueuer(downloadQueue, diskDatabase,
                    storage, commandStarter, "name")
            }
        }
        val downloadQueueCleaner = DownloadQueueCleaner(downloadQueue)
        val transactionsSender = TransactionsSender(downloadQueue)
        val regularFileStorageCleaner = RegularFileStorageCleaner(storage)
        val remoteDirChangedNotifier = RemoteDirChangedNotifier(eventSender)

        val offlineFilesSyncerFactory = object : OfflineFilesSyncerFactory {
            override fun create(localOfflineFiles: DiskFileCursor): OfflineFilesSyncer {
                return OfflineFilesSyncer(
                    diskDatabase, downloadEnqueuerFactory, downloadQueueCleaner, transactionsSender,
                    regularFileStorageCleaner, remoteDirChangedNotifier, localOfflineFiles
                )
            }
        }

        return OfflineFilesSyncOperation(diskDatabase, credentialsManagerWithUser,
            remoteRepo, offlineFilesSyncerFactory)
    }

    private fun executeOperation() {
        try {
            createOperation().execute()
        } catch (e: TemporaryException) {
            e.printStackTrace()
        } catch (e: PermanentException) {
            e.printStackTrace()
        } catch (e: SyncException) {
            e.printStackTrace()
        }

    }

}
