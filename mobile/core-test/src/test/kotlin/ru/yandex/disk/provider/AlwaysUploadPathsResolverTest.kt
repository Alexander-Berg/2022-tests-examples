package ru.yandex.disk.provider

import android.content.Context
import android.os.Environment
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import ru.yandex.disk.autoupload.observer.StorageListProvider
import ru.yandex.disk.test.Assert2.assertThat
import ru.yandex.disk.toggle.AutouploadRestrictedDirsToggle
import ru.yandex.util.Path
import java.io.File

class AlwaysUploadPathsResolverTest {

    private val resolver = AlwaysUploadPathsResolver(mock(Context::class.java), AutouploadRestrictedDirsToggle(false))
    private val storages = listOf(createStorageInfo(), createStorageInfo(1))

    @Before
    fun setup() {
        resolver.invalidate(createStorageInfo(), storages)
    }

    @Test
    fun `test always upload paths`() {
        assertIsAlwaysUploadPath(0, "DCIM")
        assertIsAlwaysUploadPath(0, "Camera")
        assertIsAlwaysUploadPath(0, "DCIM/Camera")
        assertIsAlwaysUploadPath(0, "DCIM/100ANDRO")
        assertIsAlwaysUploadPath(0, "DCIM/Screenshots1")
        assertIsAlwaysUploadPath(0, "DCIM/Screenshots/Viber")
        assertIsAlwaysUploadPath(1, "DCIM")
    }

    @Test
    fun `test not always upload paths`() {
        assertIsNotAlwaysUploadPath(0, "Pictures")
        assertIsNotAlwaysUploadPath(0, "Cymera")
        assertIsNotAlwaysUploadPath(0, "DCIM/Screenshots")
    }

    private fun assertIsAlwaysUploadPath(storageId: Int, path: String) {
        assertAlwaysUpload(createPath(storageId, path), true)
    }

    private fun assertIsNotAlwaysUploadPath(storageId: Int, path: String) {
        assertAlwaysUpload(createPath(storageId, path), false)
    }

    private fun assertAlwaysUpload(path: String, expected: Boolean) {
        val isAlwaysUpload = resolver.isAlwaysUploadPath(Path.asPath(path)!!)
        assertThat("Always upload from $path", isAlwaysUpload, equalTo(expected))
    }

    private fun createPath(storageId: Int, subPath: String): String {
        return "/storage/emulated/$storageId/$subPath"
    }

    private fun createStorageInfo(storageId: Int = 0): StorageListProvider.StorageInfo {
        val path = createPath(storageId, "")
        return StorageListProvider.StorageInfo(File(path), Environment.MEDIA_MOUNTED, storageId == 0)
    }
}
