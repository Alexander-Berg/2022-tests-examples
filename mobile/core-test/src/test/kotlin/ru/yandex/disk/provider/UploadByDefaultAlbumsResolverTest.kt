package ru.yandex.disk.provider

import android.os.Environment
import org.hamcrest.CoreMatchers
import org.junit.Before
import org.junit.Test
import ru.yandex.disk.autoupload.observer.StorageListProvider
import ru.yandex.disk.test.Assert2
import ru.yandex.disk.toggle.AutouploadRestrictedDirsToggle
import ru.yandex.util.Path
import java.io.File

class UploadByDefaultAlbumsResolverTest {

    private val resolver = UploadByDefaultAlbumsResolver(AutouploadRestrictedDirsToggle(false))
    private val primaryStorage = createStorageInfo()
    private val secondaryStorages = listOf(createStorageInfo(1))

    @Before
    fun setup() {
        resolver.invalidate(primaryStorage, secondaryStorages)
    }

    @Test
    fun testUploadByDefault() {
        assertIsUploadByDefault(0, "DCIM/Screenshots")
        assertIsUploadByDefault(1, "DCIM/Screenshots")
        assertIsUploadByDefault(0, "Cymera")
        assertIsUploadByDefault(1, "Cymera")
        assertIsUploadByDefault(0, "Pictures")
        assertIsUploadByDefault(1, "Pictures")
        assertIsUploadByDefault(0, "Pictures/Screenshots")
        assertIsUploadByDefault(1, "Pictures/Screenshots")
        assertIsUploadByDefault(0, "Images")
        assertIsUploadByDefault(0, "Videos")
        assertIsUploadByDefault(0, "Pictures/Viber")
        assertIsUploadByDefault(0, "Pictures/AnyDir")
    }

    @Test
    fun testNotUploadByDefault() {
        assertIsNotUploadByDefault(0, "Video")
        assertIsNotUploadByDefault(1, "Images")
        assertIsNotUploadByDefault(1, "Videos")
        assertIsNotUploadByDefault(0, "Cymera2")
        assertIsNotUploadByDefault(0, "MXCamera")
    }

    private fun assertIsUploadByDefault(storageId: Int, path: String) {
        assertUploadByDefault(createPath(storageId, path), true)
    }

    private fun assertIsNotUploadByDefault(storageId: Int, path: String) {
        assertUploadByDefault(createPath(storageId, path), false)
    }

    private fun assertUploadByDefault(path: String, expected: Boolean) {
        val uploadByDefault = resolver.shouldUploadByDefault(Path.asPath(path)!!)
        Assert2.assertThat("Uploading by default from $path", uploadByDefault, CoreMatchers.equalTo(expected))
    }

    private fun createPath(storageId: Int, subPath: String): String {
        return "/storage/emulated/$storageId/$subPath"
    }

    private fun createStorageInfo(storageId: Int = 0): StorageListProvider.StorageInfo {
        val path = createPath(storageId, "")
        return StorageListProvider.StorageInfo(File(path), Environment.MEDIA_MOUNTED, storageId == 0)
    }
}
