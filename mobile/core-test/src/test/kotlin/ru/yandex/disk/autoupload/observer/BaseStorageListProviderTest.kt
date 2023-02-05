package ru.yandex.disk.autoupload.observer

import android.os.Environment
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.hamcrest.Matchers.*
import ru.yandex.disk.autoupload.observer.StorageListProvider.StorageInfo
import ru.yandex.disk.test.TestCase2
import ru.yandex.disk.util.FileSystem
import ru.yandex.disk.util.TestSystem
import java.io.File
import kotlin.test.Test

class BaseStorageListProviderTest : TestCase2() {

    private var system = TestSystem()
    private var list: BaseStorageListProvider? = null
    private var mockStorageRootExists = false

    protected override fun setUp() {
        super.setUp()
        val mockStorageRoot = object : File("/storage/sdcard1") {
            override fun exists(): Boolean {
                return mockStorageRootExists
            }
        }
        val mockEmulatedStorageRoot = object : File("/storage/emulated/0") {
            override fun exists(): Boolean {
                return mockStorageRootExists
            }
        }

        val fileSystem = mock<FileSystem> {
            on { newFile("/storage/sdcard1") } doReturn mockStorageRoot
            on { newFile("/storage/emulated/0") } doReturn mockEmulatedStorageRoot
        }

        list = object : BaseStorageListProvider(mock(), system, fileSystem) {
            protected override fun getStorageState(root: File): String {
                //RobolectricTestRunner does not support API 19 (
                return Environment.MEDIA_MOUNTED
            }
        }
    }

    @Test
    fun shouldProcessSeveralStorage() {
        setenv("/storage/extSdCard:" +
                "/storage/UsbDriveA:" +
                "/storage/UsbDriveB:" +
                "/storage/UsbDriveC:" +
                "/storage/UsbDriveD:" +
                "/storage/UsbDriveE:" +
                "/storage/UsbDriveF")
        val cards = list!!.getSecondaryStorages()
        assertThat(cards, hasSize<StorageInfo>(7))
    }

    @Test
    fun shouldParseCorrectly() {
        setenv("/storage/extSdCard:" +
                "/storage/UsbDriveA:" +
                "/storage/UsbDriveB:" +
                "/storage/UsbDriveC:" +
                "/storage/UsbDriveD:" +
                "/storage/UsbDriveE:" +
                "/storage/UsbDriveF")
        val cards = list!!.getSecondaryStorages()
        assertEquals("/storage/extSdCard", cards.get(0).rootPath)
        assertEquals("/storage/UsbDriveA", cards.get(1).rootPath)
        assertEquals("/storage/UsbDriveF", cards.get(6).rootPath)
    }

    @Test
    fun shouldBeReadyForEmptyParts() {
        setenv("/storage/extSdCard:" +
                ":" +
                "/storage/UsbDriveA:")
        val cards = list!!.getSecondaryStorages()
        assertEquals(2, cards.size)
        assertEquals("/storage/extSdCard", cards.get(0).rootPath)
        assertEquals("/storage/UsbDriveA", cards.get(1).rootPath)
    }

    @Test
    fun shouldBeReadyForNullEnvProperty() {
        val cards = list!!.getSecondaryStorages()
        assertThat(cards, `is`<Collection<StorageInfo>>(empty<StorageInfo>()))
    }

    @Test
    fun shouldAddPredefinedRoot() {
        mockStorageRootExists = true

        val cards = list!!.getSecondaryStorages()
        assertThat(cards, hasSize<StorageInfo>(2))

        val sdcard1 = cards.get(0)
        assertThat(sdcard1.rootPath, equalTo<String>("/storage/sdcard1"))
        assertThat(sdcard1.isMounted(), equalTo<Boolean>(true))

        val emulated = cards.get(1)
        assertThat(emulated.rootPath, equalTo<String>("/storage/emulated/0"))
        assertThat(emulated.isMounted(), equalTo<Boolean>(true))
    }

    @Test
    fun shouldNotAddPredefinedRootIfItDoesNotExist() {
        val cards = list!!.getSecondaryStorages()

        assertThat(cards, `is`<Collection<StorageInfo>>(empty<StorageInfo>()))
    }

    @Test
    fun shouldNotAddPredefinedRootIfItIsAlreadyPresented() {
        setenv("/storage/sdcard1")
        mockStorageRootExists = true

        val cards = list!!.getSecondaryStorages()

        assertThat(cards, hasSize<StorageInfo>(2))
    }

    @Test
    fun testEmptyEnvVariable() {
        setenv("")
        val cards = list!!.getSecondaryStorages()
        assertThat(cards, `is`<Collection<StorageInfo>>(empty<StorageInfo>()))
    }

    private fun setenv(value: String) {
        system!!.setenv(BaseStorageListProvider.SECONDARY_STORAGE, value)
    }

    @Test
    fun `should define internal correctly`() {
        assertThat(StorageListProvider.unsafeIsInternal("/storage/emulated/0"), equalTo(true))
        assertThat(StorageListProvider.unsafeIsInternal("/storage/emulated/0A"), equalTo(false))
        assertThat(StorageListProvider.unsafeIsInternal("/storage/emulated/1/A/B/C"), equalTo(true))
        assertThat(StorageListProvider.unsafeIsInternal("/storage/emulated/10/A/B/C"), equalTo(true))
        assertThat(StorageListProvider.unsafeIsInternal("/storage/emulated/X/A/B/C"), equalTo(false))
        assertThat(StorageListProvider.unsafeIsInternal("/X/storage/emulated/0/A/B/C"), equalTo(false))
        assertThat(StorageListProvider.unsafeIsInternal("/sdcard/A/B/C"), equalTo(false))
    }

}
