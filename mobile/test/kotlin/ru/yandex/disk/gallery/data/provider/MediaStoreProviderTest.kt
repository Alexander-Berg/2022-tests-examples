package ru.yandex.disk.gallery.data.provider

import android.content.ContentResolver
import android.content.Context
import android.database.SQLException
import android.net.Uri
import android.provider.MediaStore
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.Matchers
import org.junit.Test
import ru.yandex.disk.provider.SafeContentResolver
import ru.yandex.disk.test.TestCase2
import ru.yandex.disk.util.Diagnostics

class MediaStoreProviderTest : TestCase2() {

    private val contentResolver = mock<ContentResolver>()
    private val diagnostics = mock<Diagnostics>()
    private val safeContentResolver = SafeContentResolver(contentResolver, diagnostics)
    private val context = mock<Context>()
    private val provider = MediaStoreProviderImpl(context, safeContentResolver)

    private val mockUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "0")

    @Test
    fun `should return null images cursor if content observer return null`() {
        assertThatArgumentIsNull(provider.getImages())
    }

    @Test
    fun `should return null videos cursor if content observer return null`() {
        assertThatArgumentIsNull(provider.getVideos())
    }

    @Test
    fun `should return null images album cursor if content observer return null`() {
        assertThatArgumentIsNull(provider.getImageAlbums())
    }

    @Test
    fun `should return null videos album cursor if content observer return null`() {
        assertThatArgumentIsNull(provider.getVideoAlbums())
    }

    @Test
    fun `should return null last image if content observer return null`() {
        assertThatArgumentIsNull(provider.getLastImage(""))
    }

    @Test
    fun `should return null last video if content observer return null`() {
        assertThatArgumentIsNull(provider.getLastVideo(""))
    }

    @Test
    fun `should return null information by source if content observer return null`() {
        assertThatArgumentIsNull(provider.getInformation(Uri.EMPTY))
    }

    @Test
    fun `should return null information by uri if content observer return null`() {
        assertThatArgumentIsNull(provider.getInformation(mockUri))
    }

    @Test
    fun `should return null item album if content observer return null`() {
        assertThatArgumentIsNull(provider.getItemAlbumId(mockUri))
    }

    @Test
    fun `should return that album is not exist if content observer return null`() {
        val albumExist = provider.albumExists("")
        assertThat(albumExist, equalTo(false))
    }

    @Test
    fun `should return empty list with paths if content observer return null`() {
        val paths = provider.queryPaths(listOf(0L), emptyList())
        assertThat(paths, Matchers.emptyCollectionOf(String::class.java))
    }

    @Test
    fun `should not fail even when contentResolver throws SQLException`() {
        whenever(contentResolver.query(any(), any(), any(), any(), any())).doThrow(SQLException())
        assertThatArgumentIsNull(provider.getImageAlbums())
    }

    private fun assertThatArgumentIsNull(any: Any?) {
        assertThat(any, nullValue())
    }
}
