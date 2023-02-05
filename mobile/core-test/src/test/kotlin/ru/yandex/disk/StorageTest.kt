@file:Suppress("IllegalIdentifier")

package ru.yandex.disk

import org.mockito.kotlin.times
import org.mockito.kotlin.verifyNoMoreInteractions
import org.hamcrest.Matchers.equalTo
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.robolectric.annotation.Config
import ru.yandex.disk.download.DownloadQueue
import ru.yandex.disk.mocks.CredentialsManagerWithUser
import ru.yandex.disk.mocks.Stubber.stub
import ru.yandex.disk.provider.DiskDatabase
import ru.yandex.disk.provider.DiskDatabase.DirectorySyncStatus.SYNCING
import ru.yandex.disk.provider.FileTree
import ru.yandex.disk.provider.FileTree.directory
import ru.yandex.disk.provider.FileTree.file
import ru.yandex.disk.service.CommandStarter
import ru.yandex.disk.settings.ApplicationSettings
import ru.yandex.disk.sql.SQLiteOpenHelper2
import ru.yandex.disk.test.AndroidTestCase2
import ru.yandex.disk.test.Reflector
import ru.yandex.disk.test.SeclusiveContext
import ru.yandex.disk.test.TestObjectsFactory
import ru.yandex.disk.upload.StorageListProviderStub
import ru.yandex.disk.util.Diagnostics
import ru.yandex.util.Path.asPath
import kotlin.test.Test

@Config(manifest = Config.NONE)
class StorageTest : AndroidTestCase2() {

    private lateinit var dbOpener: SQLiteOpenHelper2
    private lateinit var diskDatabase: DiskDatabase
    private lateinit var observer: Storage.CacheStateObserver
    private lateinit var applicationStorage: ApplicationStorage
    private lateinit var storage: Storage

    override fun setUp() {
        super.setUp()
        val context = SeclusiveContext(mContext)
        Mocks.addContentProviders(context)
        dbOpener = TestObjectsFactory.createSqlite(context)

        diskDatabase = TestObjectsFactory.createDiskDatabase(dbOpener)
        observer = mock(Storage.CacheStateObserver::class.java)
        applicationStorage = TestObjectsFactory.createApplicationStorage(context, stub(ApplicationSettings::class.java),
                stub(CredentialsManager::class.java), mock(CommandStarter::class.java),
                StorageListProviderStub(), mock(Diagnostics::class.java))
        val credentials = CredentialsManagerWithUser("test.user").activeAccountCredentials
        storage = TestObjectsFactory.createStorage(credentials, applicationStorage, diskDatabase,
                mock(DownloadQueue::class.java), observer)
    }

    @Throws(Exception::class)
    public override fun tearDown() {
        dbOpener.close()
        super.tearDown()
        Reflector.scrub(this)
    }

    @Test
    fun `should clear etag recursively`() {
        FileTree.create()
                .content(directory("A").setOffline(FileItem.OfflineMark.MARKED).setSyncStatus(SYNCING)
                        .content(file("a").setOffline(FileItem.OfflineMark.IN_OFFLINE_DIRECTORY).setEtagLocal("ETAG")))
                .insertToDiskDatabase(diskDatabase)

        storage.dropOffline()

        val fileA = diskDatabase.queryFileItem(asPath("/disk/A/a")!!)
        assertThat(fileA!!.eTagLocal, equalTo<String>(null))

        val dirA = diskDatabase.queryFileItem(asPath("/disk/A")!!)
        assertThat(dirA!!.eTagLocal, equalTo<String>(null))
    }

    @Test
    fun `should notify observer on state change`() {
        storage.dropOffline()
        verify(observer).onStateChange()
    }

    @Test
    fun `should clear user BitmapCacheCleaner list`() {
        val globalCleaner = mock(ApplicationStorage.BitmapCacheCleaner::class.java)
        applicationStorage.addBitmapCacheCleaner(globalCleaner)

        val userCleaner = mock(ApplicationStorage.BitmapCacheCleaner::class.java)
        applicationStorage.addUserBitmapCacheCleaner(userCleaner)

        storage.dropUserCachedFiles()
        verify(globalCleaner).clear()
        verify(userCleaner).clear()

        storage.dropUserCachedFiles()
        verify(globalCleaner, times(2)).clear()
        verifyNoMoreInteractions(userCleaner)
    }

}
