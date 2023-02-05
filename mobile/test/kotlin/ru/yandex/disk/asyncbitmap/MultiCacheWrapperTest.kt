package ru.yandex.disk.asyncbitmap

import com.bumptech.glide.load.Key
import com.bumptech.glide.load.engine.KeyExtractor
import com.bumptech.glide.load.engine.cache.DiskLruCacheWrapper2
import org.mockito.kotlin.*
import org.hamcrest.Matchers.equalTo
import org.junit.Test
import org.robolectric.annotation.Config
import ru.yandex.disk.ApplicationStorage
import ru.yandex.disk.Storage
import ru.yandex.disk.test.AndroidTestCase2
import ru.yandex.disk.test.TestEnvironment
import ru.yandex.disk.spaceutils.ByteUnit
import java.io.File
import javax.inject.Provider

@Config(manifest = Config.NONE)
class MultiCacheWrapperTest : AndroidTestCase2() {
    private val dir = File(TestEnvironment.getTestRootDirectory(), "storage").apply { createNewFile() }
    private val pathProvider = Provider<File> { dir }
    private val storage = mock<ApplicationStorage> {
        on { freeSpaceForUser } doReturn ByteUnit.GB.toBytes(100)
    }
    private val thumbCache = spy(DiskLruCacheWrapper2.get(pathProvider, 100) as DiskLruCacheWrapper2)
    private val tileCache = spy(DiskLruCacheWrapper2.get(pathProvider, 100) as DiskLruCacheWrapper2)
    private val commonCache = spy(DiskLruCacheWrapper2.get(pathProvider, 100) as DiskLruCacheWrapper2)
    private val faceCache = spy(DiskLruCacheWrapper2.get(pathProvider, 100) as DiskLruCacheWrapper2)
    private val size = ByteUnit.MB.toBytes(100).toInt()

    private val keyExtractor = mock<KeyExtractor>()

    private val wrapper = MultiCacheWrapper(storage,
            mapOf(
                    GlideDiskCache.THUMB to Provider { thumbCache },
                    GlideDiskCache.TILE to Provider { tileCache },
                    GlideDiskCache.COMMON to Provider { commonCache },
                    GlideDiskCache.FACE to Provider { faceCache }
            ),
            Provider { size })
            .apply { setKeyExtractor(keyExtractor) }

    private val key = mock<Key>()

    @Test
    fun `should get file from tile cache`() {
        whenever(keyExtractor.extractType(any())).thenReturn(BitmapRequest.Type.TILE)
        wrapper.get(key)

        verify(tileCache).get(key)
    }

    @Test
    fun `should get file from thumb cache`() {
        whenever(keyExtractor.extractType(any())).thenReturn(BitmapRequest.Type.THUMB)
        wrapper.get(key)

        verify(thumbCache).get(key)
    }

    @Test
    fun `should get file from common cache`() {
        whenever(keyExtractor.extractType(any())).thenReturn(BitmapRequest.Type.PREVIEW)
        wrapper.get(key)

        verify(commonCache).get(key)
    }

    @Test
    fun `should get file from face cache`() {
        whenever(keyExtractor.extractType(any())).thenReturn(BitmapRequest.Type.FACE)
        wrapper.get(key)

        verify(faceCache).get(key)
    }

    @Test
    fun `should tile cache has 80MB size`() {
        assertThat(wrapper.getMaxSize(BitmapRequest.Type.TILE), equalTo(ByteUnit.MB.toBytes(80).toInt()))
    }

    @Test
    fun `should common cache has 20MB size`() {
        //rounding error
        assertThat(wrapper.getMaxSize(BitmapRequest.Type.PREVIEW), equalTo(20971519))
    }

    @Test
    fun `should return common & tile caches max size sum`() {
        assertThat(wrapper.maxSize, equalTo(commonCache.maxSize + tileCache.maxSize))
    }

    @Test
    fun `should clear all caches`() {
        wrapper.clear()
        verify(tileCache).clear()
        verify(thumbCache).clear()
        verify(commonCache).clear()
        verify(faceCache).clear()
    }
}
