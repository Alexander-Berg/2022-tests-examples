package ru.yandex.disk.fetchfilelist

import org.mockito.kotlin.mock
import org.hamcrest.Matchers.*
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.mockito.Mockito.*
import ru.yandex.disk.*
import ru.yandex.disk.download.DownloadQueue
import ru.yandex.disk.download.DownloadQueueItem
import ru.yandex.disk.event.DiskEvents
import ru.yandex.disk.event.EventLogger
import ru.yandex.disk.mocks.CredentialsManagerWithUser
import ru.yandex.disk.mocks.FakeRemoteRepo
import ru.yandex.disk.offline.DownloadQueueCleaner
import ru.yandex.disk.offline.OfflineSyncCommandRequest
import ru.yandex.disk.offline.operations.PendingOperations
import ru.yandex.disk.offline.operations.registry.PendingOperationsRegistry
import ru.yandex.disk.provider.*
import ru.yandex.disk.provider.FileTree.directory
import ru.yandex.disk.provider.FileTree.file
import ru.yandex.disk.remote.FileParsingHandler
import ru.yandex.disk.remote.exceptions.NotFoundException
import ru.yandex.disk.remote.exceptions.RemoteExecutionException
import ru.yandex.disk.service.CommandLogger
import ru.yandex.disk.settings.ApplicationSettings
import ru.yandex.disk.settings.UserSettings
import ru.yandex.disk.test.AndroidTestCase2
import ru.yandex.disk.test.SeclusiveContext
import ru.yandex.disk.test.TestObjectsFactory
import ru.yandex.disk.util.error.ErrorReporter
import ru.yandex.util.Path
import java.util.*

class FetchFileListOperationTest : AndroidTestCase2() {
    private lateinit var mockStorage: Storage
    private lateinit var appSettings: ApplicationSettings
    private lateinit var diskDatabase: DiskDatabase
    private lateinit var operation: FetchFileListOperation
    private lateinit var fakeRemoteRepo: FakeRemoteRepo
    private lateinit var pathLockSpy: PathLockSpy
    private lateinit var downloadQueue: DownloadQueue
    private lateinit var eventLogger: EventLogger
    private lateinit var commandLogger: CommandLogger
    private lateinit var activeUser: Credentials
    private lateinit var userSettings: UserSettings
    private lateinit var pendingOperationsRegistry: PendingOperationsRegistry
    private lateinit var credentialsManager: CredentialsManagerWithUser
    private lateinit var downloadQueueCleaner: DownloadQueueCleaner
    private lateinit var regularFileStorageCleaner: RegularFileStorageCleaner
    private lateinit var errorReporter: ErrorReporter

    override fun setUp() {
        super.setUp()
        val context = SeclusiveContext(mContext)
        mockStorage = mock(Storage::class.java)

        credentialsManager = CredentialsManagerWithUser("test.user")
        activeUser = credentialsManager.activeAccountCredentials!!
        Mocks.addContentProviders(context)
        pathLockSpy = PathLockSpy()
        diskDatabase = TestObjectsFactory.createDiskDatabase(TestObjectsFactory.createSqlite(context),
                ContentChangeNotifierStub(), pathLockSpy)
        fakeRemoteRepo = FakeRemoteRepo()
        downloadQueue = TestObjectsFactory.createDownloadQueue(context)
        eventLogger = EventLogger()
        commandLogger = CommandLogger()
        appSettings = TestObjectsFactory.createApplicationSettings(context)
        userSettings = appSettings.getUserSettings(activeUser)!!
        pendingOperationsRegistry = PendingOperationsRegistry(mock(PendingOperations::class.java))
        downloadQueueCleaner = DownloadQueueCleaner(downloadQueue)
        regularFileStorageCleaner = RegularFileStorageCleaner(mockStorage)
        errorReporter = mock()
        setUpOperation(false)
    }

    private fun setUpOperation(autoCreateFolder: Boolean) {
        operation = FetchFileListOperation(credentialsManager, activeUser, fakeRemoteRepo,
                diskDatabase, eventLogger, commandLogger, userSettings, downloadQueueCleaner,
                regularFileStorageCleaner, pendingOperationsRegistry, errorReporter, autoCreateFolder)
    }

    @Test
    fun testStorageStateAfterSync() {

        val dbfile_regular_remfile_tagmatch = "dbfile_regular_remfile_tagmatch"

        val dbfile_offline_remfile_tagmatch = "dbfile_offline_remfile_tagmatch"

        val dbfile_regular_remfile_tagdiff = "dbfile_regular_remfile_tagdiff"//drop from cache
        val dbdir_regular_remfile_tagdiff = "dbdir_regular_remfile_tagdiff"//drop from cache
        val dbnone_regular_remfile_tagdiff = "dbnone_regular_remfile_tagdiff"
        val dbfile_offline_remfile_tagdiff = "dbfile_offline_remfile_tagdiff"

        val dbnone_offline_remfile_tagdiff = "dbnone_offline_remfile_tagdiff"
        val dbfile_regular_remdir_tagdiff = "dbfile_regular_remdir_tagdiff"//drop from cache
        val dbdir_regular_remdir_tagdiff = "dbdir_regular_remdir_tagdiff"
        val dbnone_regular_remdir_tagdiff = "dbnone_regular_remdir_tagdiff"
        val dbfile_offline_remdir_tagdiff = "dbfile_offline_remdir_tagdiff"

        val dbnone_offline_remdir_tagdiff = "dbnone_offline_remdir_tagdiff"
        val dbfile_regular_remnone_tagdiff = "dbfile_regular_remnone_tagdiff"//drop from cache
        val dbdir_regular_remnone_tagdiff = "dbdir_regular_remnone_tagdiff"//drop from cache

        val dbfile_offline_remnone_tagdiff = "dbfile_offline_remnone_tagdiff"//drop from cache

        val allfiles = arrayOf(dbfile_regular_remfile_tagmatch,

                dbfile_offline_remfile_tagmatch,

                dbfile_regular_remfile_tagdiff, dbdir_regular_remfile_tagdiff, dbnone_regular_remfile_tagdiff, dbfile_offline_remfile_tagdiff,

                dbnone_offline_remfile_tagdiff, dbfile_regular_remdir_tagdiff, dbdir_regular_remdir_tagdiff, dbnone_regular_remdir_tagdiff, dbfile_offline_remdir_tagdiff,

                dbnone_offline_remdir_tagdiff, dbfile_regular_remnone_tagdiff, dbdir_regular_remnone_tagdiff,

                dbfile_offline_remnone_tagdiff)

        val dbItems = ArrayList<FileTree.Item<*>>()

        for (file in allfiles) {
            if (file.contains("dbfile") || file.contains("dbdir")) {

                if (file.contains("dbfile")) {
                    val f = file(file)
                    f.setEtag(if (file.contains("tagmatch")) "tagmatch" else "dbtag")
                    f.setOffline(if (file.contains("offline")) FileItem.OfflineMark.MARKED else FileItem.OfflineMark.NOT_MARKED)
                    dbItems.add(f)
                } else {
                    val d = directory(file)
                    dbItems.add(d)
                }
            }

            if (file.contains("remfile") || file.contains("remdir")) {
                val diskItemBuilder = DiskItemBuilder().setPath("/disk/" + file)
                if (file.contains("remfile")) {
                    val etag = if (file.contains("tagmatch")) "tagmatch" else "remtag"
                    diskItemBuilder.setEtag(etag)
                } else {
                    diskItemBuilder.setIsDir(true)
                }
                fakeRemoteRepo.addDiskItems(diskItemBuilder.build())
            }
        }

        val tree = FileTree()
        tree.root().content(*dbItems.toTypedArray())
        tree.insertToDiskDatabase(diskDatabase)

        val droppedFiles = TreeSet<String>()

        doAnswer { invocationOnMock ->
            val fullpath = invocationOnMock.arguments[0] as String
            droppedFiles.add(fullpath)
            null
        }.`when`<Storage>(mockStorage).deleteFileOrFolder(anyString())

        operation.invoke("/disk")

        val expectedToDelete = TreeSet<String>()
        expectedToDelete.add("/disk/" + dbfile_regular_remfile_tagdiff)
        expectedToDelete.add("/disk/" + dbdir_regular_remfile_tagdiff)
        expectedToDelete.add("/disk/" + dbfile_regular_remdir_tagdiff)
        expectedToDelete.add("/disk/" + dbfile_regular_remnone_tagdiff)
        expectedToDelete.add("/disk/" + dbdir_regular_remnone_tagdiff)
        expectedToDelete.add("/disk/" + dbfile_offline_remnone_tagdiff)
        expectedToDelete.add("/disk/" + dbfile_offline_remdir_tagdiff)

        assertEquals(expectedToDelete, droppedFiles)
    }

    @Test
    fun testFetchFileList() {

        fakeRemoteRepo.addDiskItems(DiskItemBuilder()
                .setPath("/disk/a")
                .setPublicUrl("http://yadi.sk/d/PubliCUrL")
                .setIsDir(false)
                .setEtime(1376310208000L)
                .setHasThumbnail(true)
                .build())

        operation.invoke("/disk")

        val fromContentProvider = queryAllFiles()
        assertEquals(1, fromContentProvider.count)
        fromContentProvider.moveToFirst()

        assertEquals("http://yadi.sk/d/PubliCUrL", fromContentProvider.publicUrl)
        assertEquals(1376310208000L, fromContentProvider.etime)
        assertEquals(true, fromContentProvider.hasThumbnail)
        assertEquals(false, fromContentProvider.isDir)
        fromContentProvider.close()
    }

    @Test
    fun testBroadcastOnSuccessFetchFileList() {
        fakeRemoteRepo.addDiskItems(DiskItemFactory.create("/disk/A"))
        operation.invoke("/disk")

        val e = eventLogger.get(0) as DiskEvents.LocalCachedFileListChanged
        assertThat(e.dirPath, equalTo("/disk"))
    }

    @Test
    fun testUpdatingDirectoryShouldBeLockedWhileUpdate() {
        FileTree.create().content(directory("A")).insertToDiskDatabase(diskDatabase)
        operation.invoke("/disk/A")

        assertThat(pathLockSpy.plainLockingLog, equalTo(Arrays.asList(Path("/disk/A"))))
    }

    @Test
    fun testUpdatingDirectoryShouldBeReleaseAfterUpdate() {
        operation.invoke("/disk/A")

        assertEquals(false, pathLockSpy.isLocked("/disk/A"))
    }

    @Test
    fun testUpdatingDirectoryShouldBeReleaseAfterEvenIfExceptionDuringUpdate() {
        fakeRemoteRepo.throwException(RemoteExecutionException("test"))

        operation.invoke("/disk/A")

        assertEquals(false, pathLockSpy.isLocked("/disk/A"))
    }

    @Test
    fun testOfflineSyncCommandShouldStartIfChangesDetectedInOfflineFile() {
        val fileA = DiskItemBuilder().setPath("/disk/a").setEtag("NEW").build()
        fakeRemoteRepo.addDiskItems(fileA)

        FileTree.create().content(
                file("a").setOffline(FileItem.OfflineMark.MARKED).setEtag("ETAG")
        ).insertToDiskDatabase(diskDatabase)

        operation.invoke("/disk")

        assertThatOfflineSyncCommandStarted()
    }

    private fun assertThatOfflineSyncCommandStarted() {
        assertThat(commandLogger.get(0), instanceOf(OfflineSyncCommandRequest::class.java))
    }

    @Test
    fun testOfflineSyncCommandShouldNotStartIfOfflineFileNotModified() {
        val fileA = DiskItemBuilder().setPath("/disk/a").setEtag("ETAG").build()
        fakeRemoteRepo.addDiskItems(fileA)

        FileTree.create().content(
                file("a").setOffline(FileItem.OfflineMark.MARKED).setEtag("ETAG")
        ).insertToDiskDatabase(diskDatabase)

        operation.invoke("/disk")

        assertThat("expect no started services", commandLogger.count, equalTo(0))
    }

    @Test
    fun testOfflineSyncCommandShouldStartOnce() {
        val fileA = DiskItemBuilder().setPath("/disk/a").setEtag("NEW").build()
        val fileB = DiskItemBuilder().setPath("/disk/b").setEtag("NEW").build()

        fakeRemoteRepo.addDiskItems(fileA)
        fakeRemoteRepo.addDiskItems(fileB)

        FileTree.create().content(
                file("a").setOffline(FileItem.OfflineMark.MARKED).setEtag("ETAG"),
                file("b").setOffline(FileItem.OfflineMark.MARKED).setEtag("ETAG")
        ).insertToDiskDatabase(diskDatabase)

        operation.invoke("/disk")

        assertThat(commandLogger.count, equalTo(2))
    }

    @Test
    fun testOfflineSyncCommandShouldStartIfOfflineFileDeletedOnServer() {
        FileTree.create().content(
                file("a").setOffline(FileItem.OfflineMark.MARKED).setEtag("ETAG")
        ).insertToDiskDatabase(diskDatabase)

        operation.invoke("/disk")

        assertThatOfflineSyncCommandStarted()
    }

    @Test
    fun testDeletedFileShouldBeRemovedFromDownloadQueue() {
        addSyncDownloadQueueItem()

        FileTree.create().content(
                file("a").setOffline(FileItem.OfflineMark.MARKED).setEtag("ETAG")
        ).insertToDiskDatabase(diskDatabase)

        operation.invoke("/disk")

        assertTrue("!downloadQueue.isEmpty()", downloadQueue.isEmpty)
    }

    private fun testSyncShouldBeStartedIfFileInOfflineDirCreated(offlineMark: FileItem.OfflineMark) {
        FileTree.create()
                .content(directory("OfflineDir").setOffline(offlineMark))
                .insertToDiskDatabase(diskDatabase)

        val file = DiskItemBuilder()
                .setPath("/disk/OfflineDir/file.txt").setIsDir(false)
                .build()

        fakeRemoteRepo.addDiskItems(file)

        operation.invoke("/disk/OfflineDir")

        assertThat(commandLogger.findByClass(OfflineSyncCommandRequest::class.java), notNullValue())
    }

    private fun testSyncShouldBeStartedIfFileInOfflineDirChanged(offlineMark: FileItem.OfflineMark) {
        FileTree.create()
                .content(directory("OfflineDir").setOffline(offlineMark)
                        .content(file("file.txt").setEtag("ETAG_BEFORE"))
                )
                .insertToDiskDatabase(diskDatabase)

        val file = DiskItemBuilder()
                .setPath("/disk/OfflineDir/file.txt")
                .setEtag("ETAG_AFTER")
                .build()

        fakeRemoteRepo.addDiskItems(file)

        operation.invoke("/disk/OfflineDir")

        assertThat(commandLogger.findByClass(OfflineSyncCommandRequest::class.java), notNullValue())
    }

    private fun testSyncShouldBeStartedIfFileInOfflineDirDeleted(offlineMark: FileItem.OfflineMark) {
        FileTree.create()
                .content(directory("OfflineDir").setOffline(offlineMark)
                        .content(file("file.txt"))
                )
                .insertToDiskDatabase(diskDatabase)

        operation.invoke("/disk/OfflineDir")

        assertThat(commandLogger.findByClass(OfflineSyncCommandRequest::class.java), notNullValue())
    }

    private fun testSyncShouldBeStartedIfDirInOfflineDirCreated(offlineMark: FileItem.OfflineMark) {
        FileTree.create()
                .content(directory("OfflineDir").setOffline(offlineMark))
                .insertToDiskDatabase(diskDatabase)

        val dir = DiskItemBuilder()
                .setPath("/disk/OfflineDir/dir").setIsDir(true)
                .build()

        fakeRemoteRepo.addDiskItems(dir)

        operation.invoke("/disk/OfflineDir")

        assertThat(commandLogger.findByClass(OfflineSyncCommandRequest::class.java), notNullValue())
    }

    private fun testSyncShouldBeStartedIfDirInOfflineDirDeleted(offlineMark: FileItem.OfflineMark) {
        FileTree.create()
                .content(directory("OfflineDir").setOffline(offlineMark)
                        .content(directory("dir"))
                )
                .insertToDiskDatabase(diskDatabase)
        operation.invoke("/disk/OfflineDir")

        assertThat(commandLogger.findByClass(OfflineSyncCommandRequest::class.java), notNullValue())
    }

    @Test
    fun testSyncShouldBeStartedIfOfflineFileInRegularDirChanged() {
        FileTree.create().content(directory("A")
                .content(file("file.txt").setEtag("ETAG_BEFORE").setOffline(FileItem.OfflineMark.MARKED)))
                .insertToDiskDatabase(diskDatabase)

        val file = DiskItemBuilder()
                .setPath("/disk/A/file.txt")
                .setEtag("ETAG_AFTER")
                .build()

        fakeRemoteRepo.addDiskItems(file)

        operation.invoke("/disk/A")

        assertThat(commandLogger.findByClass(OfflineSyncCommandRequest::class.java), notNullValue())
    }

    private fun setupFileNotFound() {
        `when`(fakeRemoteRepo.getFileList(anyString(), anyInt(), anySortOrder(), anyFileParsingHandler()))
                .thenThrow(NotFoundException("Dir not found"))
    }

    private fun testEmptyFolderAutoСreated(autoCreated: Boolean) {
        setupFileNotFound()
        val expectedTimes = if (autoCreated) 1 else 0

        operation.invoke("/disk/A")

        verify(fakeRemoteRepo, times(expectedTimes)).makeFolder(anyPatch())
    }

    private fun testFetchRequestedOnEmptyFolderOneTime() {
        setupFileNotFound()

        operation.invoke("/disk/A")

        verify(fakeRemoteRepo, times(1)).getFileList(anyString(), anyInt(), anySortOrder(),
                anyFileParsingHandler())
    }

    private fun setUpWithMockRemoteRepo(autoCreateFolder: Boolean) {
        fakeRemoteRepo = mock(FakeRemoteRepo::class.java)
        setUpOperation(autoCreateFolder)
    }

    @Test
    fun `should not auto create if auto create folder parameter not set`() {
        setUpWithMockRemoteRepo(false)

        testEmptyFolderAutoСreated(false)
    }

    @Test
    fun `should auto create dir if not exist`() {
        setUpWithMockRemoteRepo(true)

        testEmptyFolderAutoСreated(true)
    }

    @Test
    fun `should run fetch only one time if dir exist`() {
        setUpWithMockRemoteRepo(false)

        testFetchRequestedOnEmptyFolderOneTime()
    }

    @Test
    fun `should run fetch only one time if dir not exist`() {
        setUpWithMockRemoteRepo(true)

        testFetchRequestedOnEmptyFolderOneTime()
    }

    @Test
    fun `should ignore exceptions of creating dir`() {
        setUpWithMockRemoteRepo(true)
        `when`(fakeRemoteRepo.makeFolder(anyPatch())).thenThrow(RemoteExecutionException("Already exist!"))

        testFetchRequestedOnEmptyFolderOneTime()
    }

    @Test
    fun `should send only one not found event`() {
        setUpWithMockRemoteRepo(true)
        setupFileNotFound()

        operation.invoke("/disk/A")

        assertThat(eventLogger.count, equalTo(1))
        val fetchFailedEvent = eventLogger.get(0) as DiskEvents.FetchFileListFailed
        assertThat(fetchFailedEvent.isNotFound, equalTo(true))
    }

    @Test
    fun testSyncShouldBeStartedIfFileInOfflineDirCreated1() {
        testSyncShouldBeStartedIfFileInOfflineDirCreated(FileItem.OfflineMark.MARKED)
    }

    @Test
    fun testSyncShouldBeStartedIfFileInOfflineDirChanged1() {
        testSyncShouldBeStartedIfFileInOfflineDirChanged(FileItem.OfflineMark.MARKED)
    }

    @Test
    fun testSyncShouldBeStartedIfFileInOfflineDirDeleted1() {
        testSyncShouldBeStartedIfFileInOfflineDirDeleted(FileItem.OfflineMark.MARKED)
    }

    @Test
    fun testSyncShouldBeStartedIfDirInOfflineDirCreated1() {
        testSyncShouldBeStartedIfDirInOfflineDirCreated(FileItem.OfflineMark.MARKED)
    }

    @Test
    fun testSyncShouldBeStartedIfDirInOfflineDirDeleted1() {
        testSyncShouldBeStartedIfDirInOfflineDirDeleted(FileItem.OfflineMark.MARKED)
    }

    @Test
    fun testSyncShouldBeStartedIfFileInOfflineDirCreated2() {
        testSyncShouldBeStartedIfFileInOfflineDirCreated(FileItem.OfflineMark.IN_OFFLINE_DIRECTORY)
    }

    @Test
    fun testSyncShouldBeStartedIfFileInOfflineDirChanged2() {
        testSyncShouldBeStartedIfFileInOfflineDirChanged(FileItem.OfflineMark.IN_OFFLINE_DIRECTORY)

    }

    @Test
    fun testSyncShouldBeStartedIfFileInOfflineDirDeleted2() {
        testSyncShouldBeStartedIfFileInOfflineDirDeleted(FileItem.OfflineMark.IN_OFFLINE_DIRECTORY)
    }

    @Test
    fun testSyncShouldBeStartedIfDirInOfflineDirCreated2() {
        testSyncShouldBeStartedIfDirInOfflineDirCreated(FileItem.OfflineMark.IN_OFFLINE_DIRECTORY)
    }

    @Test
    fun testSyncShouldBeStartedIfDirInOfflineDirDeleted2() {
        testSyncShouldBeStartedIfDirInOfflineDirDeleted(FileItem.OfflineMark.IN_OFFLINE_DIRECTORY)
    }

    @Test
    fun `should clear default folder settings if target folder is not found`() {
        userSettings.defaultFilesPartitionDir = "/disk/A"
        setUpWithMockRemoteRepo(false)
        setupFileNotFound()

        operation.invoke("/disk/A")

        assertThat(userSettings.defaultFilesPartitionDir, nullValue())
    }

    @Test
    fun `should not clear default folder settings if not founded folder is not match`() {
        userSettings.defaultFilesPartitionDir = "/disk/A"
        setUpWithMockRemoteRepo(false)
        setupFileNotFound()

        operation.invoke("/disk/B")

        assertThat(userSettings.defaultFilesPartitionDir, equalTo("/disk/A"))
    }

    @Test
    fun `should not clear default folder settings if target folder exist`() {
        userSettings.defaultFilesPartitionDir = "/disk/A"
        setUpWithMockRemoteRepo(false)

        operation.invoke("/disk/A")

        assertThat(userSettings.defaultFilesPartitionDir, equalTo("/disk/A"))
    }

    private fun queryAllFiles(): DiskFileCursor {
        return diskDatabase.queryAll()
    }

    private fun addSyncDownloadQueueItem() {
        downloadQueue.add(DownloadQueueItem.Type.SYNC, Path("/disk/a"), null, 0, 0)
    }

    private fun anySortOrder() = Mockito.any(SortOrder::class.java)

    private fun anyFileParsingHandler() = Mockito.any(FileParsingHandler::class.java)

    private fun anyPatch() = Mockito.any(Path::class.java)

    fun FetchFileListOperation.invoke(dirToList: String) {
        return invoke(dirToList, false)
    }
}
