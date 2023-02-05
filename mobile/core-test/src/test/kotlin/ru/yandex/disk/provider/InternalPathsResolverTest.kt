package ru.yandex.disk.provider

import android.os.Environment
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import ru.yandex.disk.autoupload.observer.StorageListProvider
import ru.yandex.util.Path
import java.io.File

class InternalPathsResolverTest {

    private val resolver = InternalPathsResolver()
    private val storages = listOf(createStorageInfo(), createStorageInfo(1))

    @Before
    fun setup() {
        resolver.invalidate(storages)
    }

    @Test
    fun `test internal paths`() {
        assertIsInternalPath(0, "DCIM")
        assertIsInternalPath(0, "")
        assertIsInternalPath(0, "Camera")
        assertIsInternalPath(0, "Android/data/ru.yandex.disk")
        assertIsInternalPath("/data/data/app/ru.yandex.disk")
        assertIsInternalPath("/data/local")
    }

    @Test
    fun `test not internal paths`() {
        assertIsNotInternalPath(1, "Pictures")
        assertIsNotInternalPath(1, "")
    }

    private fun assertIsInternalPath(path: String) {
        assertInternalPath(path, true)
    }

    private fun assertIsInternalPath(storageId: Int, path: String) {
        assertIsInternalPath(createPath(storageId, path))
    }

    private fun assertIsNotInternalPath(storageId: Int, path: String) {
        assertInternalPath(createPath(storageId, path), false)
    }

    private fun assertInternalPath(path: String, expected: Boolean) {
        val isAlwaysUpload = resolver.isInternalPath(Path.asPath(path)!!)
        assertThat("Internal $path", isAlwaysUpload, equalTo(expected))
    }

    private fun createPath(storageId: Int, subPath: String): String {
        return "/storage/emulated/$storageId/$subPath"
    }

    private fun createStorageInfo(storageId: Int = 0): StorageListProvider.StorageInfo {
        val path = createPath(storageId, "")
        return StorageListProvider.StorageInfo(File(path), Environment.MEDIA_MOUNTED, storageId == 0)
    }
}