package ru.yandex.disk.cache

import org.mockito.kotlin.whenever
import com.yandex.disk.client.Hash
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.instanceOf
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyObject
import org.mockito.Mockito.mock
import org.robolectric.annotation.Config
import ru.yandex.disk.Credentials
import ru.yandex.disk.CredentialsManager
import ru.yandex.disk.DeveloperSettings
import ru.yandex.disk.Storage
import ru.yandex.disk.asyncbitmap.BitmapCacheWrapper
import ru.yandex.disk.asyncbitmap.BitmapCacheWrapperStub
import ru.yandex.disk.download.DownloadQueue
import ru.yandex.disk.event.DiskEvents
import ru.yandex.disk.event.EventLogger
import ru.yandex.disk.imports.ImportingFilesStorage
import ru.yandex.disk.provider.DiskDatabase
import ru.yandex.disk.provider.FileTree
import ru.yandex.disk.provider.FileTree.directory
import ru.yandex.disk.provider.FileTree.file
import ru.yandex.disk.service.CommandLogger
import ru.yandex.disk.settings.ApplicationSettings
import ru.yandex.disk.settings.UserSettings
import ru.yandex.disk.sql.SQLiteOpenHelper2
import ru.yandex.disk.test.AndroidTestCase2
import ru.yandex.disk.test.SeclusiveContext
import ru.yandex.disk.test.TestObjectsFactory
import ru.yandex.disk.upload.UploadQueue
import ru.yandex.disk.util.Diagnostics
import ru.yandex.disk.util.FileContentAccessor
import ru.yandex.disk.util.FileSystem
import ru.yandex.disk.util.Files
import java.io.File

private const val STORAGE_ID_FILE_NAME = ".storageId"

@Config(manifest = Config.NONE)
class ChangeCachePartitionCommandTest : AndroidTestCase2() {

    private lateinit var context: SeclusiveContext
    private lateinit var dbOpener: SQLiteOpenHelper2
    private lateinit var diskDatabase: DiskDatabase
    private lateinit var eventLogger: EventLogger
    private lateinit var fileSystem: FileSystem
    private lateinit var command: ChangeCachePartitionCommand

    private lateinit var internalStorage: File
    private lateinit var externalStorage: File
    private lateinit var storage: StorageStub
    private lateinit var appSettings: ApplicationSettings

    private val testExternalDirectory: File
        get() {
            return makeCacheDir("external")
        }

    private val testInternalDirectory: File
        get() {
            return makeCacheDir("internal")
        }

    private fun makeCacheDir(name: String): File {
        return File("data", name).apply {
            mkdirs()
        }
    }

    @Before
    public override fun setUp() {
        super.setUp()
        internalStorage = testInternalDirectory
        externalStorage = testExternalDirectory

        context = SeclusiveContext(mContext)

        appSettings = mock(ApplicationSettings::class.java)
        whenever(appSettings.cachePartition).thenReturn(testInternalDirectory.path)

        val cm = mock(CredentialsManager::class.java)
        val user = mock(Credentials::class.java)
        val userSettings = mock(UserSettings::class.java)
        whenever(cm.activeAccountCredentials).thenReturn(user)
        whenever(appSettings.getUserSettings(any())).thenReturn(userSettings)
        whenever(userSettings.userInstallId).thenReturn("98765")

        dbOpener = TestObjectsFactory.createSqlite(context)
        diskDatabase = TestObjectsFactory.createDiskDatabase(dbOpener)
        val diagnostics = mock(Diagnostics::class.java)

        storage = StorageStub(context, diskDatabase, null, appSettings, cm, null,
                diagnostics, mock(DeveloperSettings::class.java))
        storage.storageDir = internalStorage
        context.singletons.register(Storage::class.java, storage)

        BitmapCacheWrapper.setImpl(BitmapCacheWrapperStub())

        fileSystem = FileSystem()
        eventLogger = EventLogger()
        val devSettings = mock<DeveloperSettings>(DeveloperSettings::class.java)
        whenever(devSettings.minLimitFreeSpaceMb).thenReturn(100)


        command = ChangeCachePartitionCommand(CommandLogger(),
                mock(UploadQueue::class.java), mock(DownloadQueue::class.java), eventLogger,
                storage, appSettings, fileSystem, diskDatabase, devSettings)
    }

    @Test
    fun shouldCopyIfDestStorageIsEmpty() {
        val tree = FileTree("disk")
        tree.root().content(file("file2"),
                directory("folder").content(
                        file("file1")
                ))
        tree.createInFileSystem(internalStorage)
        tree.insertToDiskDatabase(diskDatabase)

        command.execute(ChangeCachePartitionCommandRequest(externalStorage.path, true))

        val filesInInternal = Files.getFileListRecursive(internalStorage, false)
        assertThat(filesInInternal.size, equalTo(0))

        val filePathsInInternal = tree.asPathList()
        val filesInExternal = Files.getFileListRecursive(externalStorage, false)
        for (path in filePathsInInternal) {
            assertTrue(filesInExternal.contains(File(externalStorage, path)))
        }
    }

    @Test
    fun shouldDeleteIfFileExistsInDestButNotExistsInSrc() {
        val srcTree = FileTree("disk")
        srcTree.root().content(file("file2"))
        srcTree.createInFileSystem(internalStorage)
        srcTree.insertToDiskDatabase(diskDatabase)

        val destTree = FileTree("disk")
        destTree.root().content(file("file1"))
        destTree.createInFileSystem(externalStorage)

        command.execute(ChangeCachePartitionCommandRequest(externalStorage.path, true))

        val filesInInternal = Files.getFileListRecursive(internalStorage, false)
        assertThat(filesInInternal.size, equalTo(0))

        val filesInExternal = Files.getFileListRecursive(externalStorage, false)

        assertTrue(filesInExternal.contains(
                File(externalStorage, srcTree.getPathForItemAt(0))))
        assertFalse(filesInExternal.contains(
                File(externalStorage, destTree.getPathForItemAt(0))))
    }

    @Test
    fun shouldRewriteIfSrcAndDestFilesAreNotTheSame() {
        val srcTree = FileTree("disk")
        val srcFile = file("file.txt")
        srcTree.root().content(srcFile.setEtagLocal("ETAG"))
        srcTree.createInFileSystem(internalStorage)
        srcTree.insertToDiskDatabase(diskDatabase)
        FileContentAccessor(internalStorage).write(srcFile.path, "ABC")

        val destTree = FileTree("disk")
        val destFile = file("file.txt")
        destTree.root().content(destFile)
        destTree.createInFileSystem(externalStorage)
        val contentAccessor = FileContentAccessor(externalStorage)
        contentAccessor.write(destFile.path, "DEF")
        whenever(mock(Hash::class.java).md5).thenReturn("MD5")

        command.execute(ChangeCachePartitionCommandRequest(externalStorage.path, true))

        val filesInInternal = Files.getFileListRecursive(internalStorage, false)
        assertThat(filesInInternal.size, equalTo(0))

        val filePathsInInternal = srcTree.asPathList()
        val filesInExternal = Files.getFileListRecursive(externalStorage, false)
        for (path in filePathsInInternal) {
            assertTrue(filesInExternal.contains(File(externalStorage, path)))
        }

        assertThat(contentAccessor.read(destFile.path), equalTo("ABC"))
    }

    @Test
    fun shouldKeepIfSrcAndDestFilesAreTheSame() {
        storage.updateCacheStorageId(externalStorage.path)
        val srcTree = FileTree("disk")
        val srcFile = file("file.txt")
        srcTree.root().content(srcFile.setEtag("822dd494b3e14a82aa76bd455e6b6f4b").setSize(3))
        srcTree.createInFileSystem(internalStorage)
        srcTree.insertToDiskDatabase(diskDatabase)
        FileContentAccessor(internalStorage).write(srcFile.path, "ABC")

        val destTree = FileTree("disk")
        val destFile = file("file.txt")
        destTree.root().content(destFile)
        destTree.createInFileSystem(externalStorage)
        val contentAccessor = FileContentAccessor(externalStorage)
        contentAccessor.write(destFile.path, "DEF")

        storage.setCurrentPartitionAsInternal(true)
        whenever(appSettings.cachePartition).thenReturn(testInternalDirectory.path)

        command.execute(ChangeCachePartitionCommandRequest(externalStorage.path, true))

        val filesInInternal = Files.getFileListRecursive(internalStorage, false)
        assertThat(filesInInternal.size, equalTo(0))

        val filePathsInInternal = srcTree.asPathList()
        val filesInExternal = Files.getFileListRecursive(externalStorage, false)
        for (path in filePathsInInternal) {
            assertTrue(filesInExternal.contains(File(externalStorage, path)))
        }

        assertThat(contentAccessor.read(destFile.path), equalTo("DEF"))
    }

    @Test
    fun shouldStartMergeWithoutErrorIfChangeExternalToInternal() {
        val srcTree = FileTree("disk")
        srcTree.root().content(file("file.txt"))
        srcTree.createInFileSystem(externalStorage)
        srcTree.insertToDiskDatabase(diskDatabase)

        val destTree = FileTree("disk")
        destTree.root().content(file("file.txt"))
        destTree.createInFileSystem(internalStorage)

        storage.storageDir = externalStorage
        whenever(appSettings.cachePartition).thenReturn(externalStorage.path)

        command.execute(ChangeCachePartitionCommandRequest(internalStorage.path, false))
        assertNull(eventLogger.findByClass(DiskEvents.ChangeCachePartitionDestExistsError::class.java))
    }

    @Test
    fun shouldSendErrorIfChangeInternalToExternalAndUuidAreNotEquals() {
        val srcTree = FileTree("disk")
        srcTree.root().content(file("file.txt"))
        srcTree.createInFileSystem(internalStorage)
        srcTree.insertToDiskDatabase(diskDatabase)

        val destTree = FileTree("disk")
        destTree.root().content(file("file.txt"))
        destTree.createInFileSystem(externalStorage)

        val storageIdFile = File(externalStorage, STORAGE_ID_FILE_NAME)
        storageIdFile.createNewFile()

        Files.writeString(storageIdFile, "12345")

        storage.setCurrentPartitionAsInternal(true)

        command.execute(ChangeCachePartitionCommandRequest(externalStorage.path, false))

        assertThat(eventLogger.get(1),
                instanceOf(DiskEvents.ChangeCachePartitionDestExistsError::class.java))
    }

    @Test
    fun shouldNotSendErrorIfChangeInternalToExternalAndDestNotExistsAndStorageIdFileNotExists() {
        val srcTree = FileTree("disk")
        srcTree.root().content(file("file.txt"))
        srcTree.createInFileSystem(internalStorage)
        srcTree.insertToDiskDatabase(diskDatabase)

        storage.setCurrentPartitionAsInternal(true)
        command.execute(ChangeCachePartitionCommandRequest(externalStorage.path, false))

        assertNull(eventLogger.findByClass(DiskEvents.ChangeCachePartitionDestExistsError::class.java))
    }

    @Test
    fun shouldSendErrorIfChangeInternalToExternalAndDestExistsAndStorageIdFileNotExists() {
        val srcTree = FileTree("disk")
        srcTree.root().content(file("file.txt"))
        srcTree.createInFileSystem(internalStorage)
        srcTree.insertToDiskDatabase(diskDatabase)

        val destTree = FileTree("disk")
        destTree.root().content(file("file.txt"),
                directory("dir"))
        destTree.createInFileSystem(externalStorage)
        File(externalStorage, Storage.NOMEDIA_FILE_NAME).createNewFile()

        storage.setCurrentPartitionAsInternal(true)
        command.execute(ChangeCachePartitionCommandRequest(externalStorage.path, false))

        assertThat(eventLogger.get(1),
                instanceOf(DiskEvents.ChangeCachePartitionDestExistsError::class.java))
    }

    @Test
    fun shouldStartMergeIfChangeInternalToExternalAndUuidAreEquals() {
        val srcTree = FileTree("disk")
        srcTree.root().content(file("file.txt"))
        srcTree.createInFileSystem(internalStorage)
        srcTree.insertToDiskDatabase(diskDatabase)

        val destTree = FileTree("disk")
        destTree.root().content(file("file.txt"))
        destTree.createInFileSystem(externalStorage)

        storage.setCurrentPartitionAsInternal(true)

        val storageIdFile = File(externalStorage, STORAGE_ID_FILE_NAME)
        storageIdFile.createNewFile()
        Files.writeString(storageIdFile, "98765")

        command.execute(ChangeCachePartitionCommandRequest(externalStorage.path, false))

        assertNull(eventLogger.findByClass(DiskEvents.ChangeCachePartitionDestExistsError::class.java))
    }

    @Test
    fun shouldNotCopyPartialFiles() {
        val srcTree = FileTree("disk")
        srcTree.root().content(
                file("file2"),
                file("file3.partial.1234"),
                directory("folder").content(
                        file("file1.partial.6789")
                ))
        srcTree.createInFileSystem(internalStorage)

        command.execute(ChangeCachePartitionCommandRequest(externalStorage.path, true))

        val filesInInternal = Files.getFileListRecursive(internalStorage, false)
        assertThat(filesInInternal.size, equalTo(0))

        val filesInExternal = Files.getFileListRecursive(externalStorage, false)
        assertTrue(filesInExternal.contains(
                File(externalStorage, srcTree.getPathForItemAt(0))))
        assertFalse(filesInExternal.contains(
                File(externalStorage, srcTree.getPathForItemAt(1))))

        val file = (srcTree.itemAt(2) as FileTree.Directory).fileAt(0)
        assertFalse(filesInExternal.contains(File(externalStorage, file.path)))
    }

    @Test
    fun `should copy bitmap cache dirs on cache change`() {
        File(internalStorage, Storage.TILE_CACHE_DIR).mkdirs()
        File(internalStorage, Storage.THUMB_CACHE_DIR).mkdirs()
        File(internalStorage, Storage.COMMON_CACHE_DIR).mkdirs()

        command.execute(ChangeCachePartitionCommandRequest(externalStorage.path, true))

        assertExternalStorageFilesCount(1)

        assertThat(File(externalStorage, Storage.BITMAP_CACHE_DIR_ROOT).listFiles().size, equalTo(3))
        assertThat(File(externalStorage, Storage.TILE_CACHE_DIR).exists(), equalTo(true))
        assertThat(File(externalStorage, Storage.THUMB_CACHE_DIR).exists(), equalTo(true))
        assertThat(File(externalStorage, Storage.COMMON_CACHE_DIR).exists(), equalTo(true))
    }

    @Test
    fun `should copy files cache dirs on cache change`() {
        File(internalStorage, Storage.DISK_PATH).mkdirs()
        File(internalStorage, Storage.OFFLINE_TEMP_PATH).mkdirs()
        File(internalStorage, Storage.OFFLINE_PHOTOUNLIM_TEMP_PATH).mkdirs()
        File(internalStorage, Storage.EDITOR_TEMP_PATH).mkdirs()
        File(internalStorage, ImportingFilesStorage.UPLOADS_PATH).mkdirs()

        command.execute(ChangeCachePartitionCommandRequest(externalStorage.path, true))

        assertExternalStorageFilesCount(5)

        assertThat(File(externalStorage, Storage.DISK_PATH).exists(), equalTo(true))
        assertThat(File(externalStorage, Storage.OFFLINE_TEMP_PATH).exists(), equalTo(true))
        assertThat(File(externalStorage, Storage.OFFLINE_PHOTOUNLIM_TEMP_PATH).exists(), equalTo(true))
        assertThat(File(externalStorage, Storage.EDITOR_TEMP_PATH).exists(), equalTo(true))
        assertThat(File(externalStorage, ImportingFilesStorage.UPLOADS_PATH).exists(), equalTo(true))
    }

    private fun assertExternalStorageFilesCount(count: Int) {
        val existingFilesCount = getExternalStorageFilesCount()
        assertThat(existingFilesCount, equalTo(count))
    }

    private fun getExternalStorageFilesCount() = externalStorage.listFiles()
            .filter { it.name != STORAGE_ID_FILE_NAME }
            .filter { it.exists() }.size

    public override fun tearDown() {
        dbOpener.close()
        Files.dropAllFilesInDir(externalStorage)
        Files.dropAllFilesInDir(internalStorage)
        super.tearDown()
    }

}
