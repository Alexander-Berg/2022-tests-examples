package com.edadeal.android.util

import com.edadeal.android.util.picasso.PicassoCacheOptions
import com.edadeal.okhttp3.DiskCacheAffinity
import okhttp3.CacheControl
import okhttp3.HttpUrl
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

class PicassoCacheOptionsTest {

    @Test
    fun `fromJson should return same options serialized with toJson`() {
        val custom = PicassoCacheOptions(DiskCacheAffinity.Custom("id"), CacheControl.FORCE_CACHE.toString())
        val empty = PicassoCacheOptions(DiskCacheAffinity.Default, null)

        assertEquals(custom, PicassoCacheOptions.fromJson(custom.toJson()))
        assertEquals(empty, PicassoCacheOptions.fromJson(empty.toJson()))
    }

    @Test
    fun `takeQueryCacheOptions should return value from setQueryCacheOptions`() {
        val url = HttpUrl.get("https://s3.yandex.net/37750.jpg?res=xxl")
        val options = PicassoCacheOptions(DiskCacheAffinity.Custom("id"), CacheControl.FORCE_CACHE.toString())

        val tmp = PicassoCacheOptions.setQueryCacheOptions(url, options.toJson())
        val (newUrl, json) = PicassoCacheOptions.takeQueryCacheOptions(tmp)
        assertNotNull(json)
        assertEquals(options, PicassoCacheOptions.fromJson(json))
        assertEquals(url, newUrl)
    }

    @Test
    fun `takeQueryCacheOptions should return same url if cache options are not set`() {
        val url = HttpUrl.get("https://s3.yandex.net/37750.jpg?res=xxl")

        val (newUrl, json) = PicassoCacheOptions.takeQueryCacheOptions(url)
        assertNull(json)
        assertSame(url, newUrl)
    }

    @Test
    fun `setQueryCacheOptions should return same url if query does not contains conflicting parameter names`() {
        val url = HttpUrl.get("https://s3.yandex.net/37750.jpg?res=xxl")

        assertSame(url, PicassoCacheOptions.setQueryCacheOptions(url, null))
    }

    @Test
    fun `setQueryCacheOptions should not replace existing query parameters`() {
        val url = HttpUrl.Builder()
            .scheme("https")
            .host("s3.yandex.net")
            .addPathSegment("37750.jpg")
            .addQueryParameter("res", "xxl")
            .addQueryParameter("picasso_cache_options9001", "{\"k\":\"v\"}")
            .build()
        val options = PicassoCacheOptions(DiskCacheAffinity.Default, null)

        val tmp = PicassoCacheOptions.setQueryCacheOptions(url, options.toJson())
        val (newUrl, _) = PicassoCacheOptions.takeQueryCacheOptions(tmp)
        assertEquals(url, newUrl)
    }

    @Test
    fun `setQueryCacheOptions should add empty parameter if query contains conflicting parameter names`() {
        val url = HttpUrl.get("https://s3.yandex.net/37750.jpg?res=xxl&picasso_cache_options9001=%7B%7D")

        val tmp = PicassoCacheOptions.setQueryCacheOptions(url, null)
        val (newUrl, json) = PicassoCacheOptions.takeQueryCacheOptions(tmp)
        assertNull(json)
        assertEquals(url, newUrl)
        assertEquals(listOf(null), tmp.queryParameterValues("picasso_cache_options9002"))
    }
}
