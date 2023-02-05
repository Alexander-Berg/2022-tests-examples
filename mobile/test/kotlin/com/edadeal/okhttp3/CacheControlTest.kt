package com.edadeal.okhttp3

import okhttp3.Headers
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CacheControlTest {

    @Test
    fun `empty builder should return default values for cache control`() {
        val cacheControl = CacheControl.Builder().build()

        assertEquals("", cacheControl.toString())
        assertFalse(cacheControl.noCache)
        assertFalse(cacheControl.noStore)
        assertFalse(cacheControl.isPublic)
        assertFalse(cacheControl.isPrivate)
        assertFalse(cacheControl.immutable)
        assertFalse(cacheControl.noTransform)
        assertFalse(cacheControl.onlyIfCached)
        assertFalse(cacheControl.mustRevalidate)
        assertNull(cacheControl.maxAgeSeconds)
        assertNull(cacheControl.maxStaleSeconds)
        assertNull(cacheControl.minFreshSeconds)
        assertNull(cacheControl.staleIfErrorSeconds)
        assertNull(cacheControl.staleWhileRevalidateSeconds)
    }

    @Test
    fun `builder should set correct values for cache control`() {
        val cacheControl = CacheControl.Builder()
            .noCache()
            .noStore()
            .maxAge(1, TimeUnit.SECONDS)
            .maxStale(2, TimeUnit.SECONDS)
            .minFresh(3, TimeUnit.SECONDS)
            .staleIfError(4, TimeUnit.SECONDS)
            .staleWhileRevalidate(5, TimeUnit.SECONDS)
            .onlyIfCached()
            .build()
        val headerValue = "no-cache, no-store, max-age=1, max-stale=2, min-fresh=3, stale-if-error=4, " +
            "stale-while-revalidate=5, only-if-cached"

        assertEquals(headerValue, cacheControl.toString())
        assertTrue(cacheControl.noCache)
        assertTrue(cacheControl.noStore)
        assertTrue(cacheControl.onlyIfCached)
        assertFalse(cacheControl.isPublic)
        assertFalse(cacheControl.isPrivate)
        assertFalse(cacheControl.immutable)
        assertFalse(cacheControl.noTransform)
        assertFalse(cacheControl.mustRevalidate)
        assertEquals(1, cacheControl.maxAgeSeconds)
        assertEquals(2, cacheControl.maxStaleSeconds)
        assertEquals(3, cacheControl.minFreshSeconds)
        assertEquals(4, cacheControl.staleIfErrorSeconds)
        assertEquals(5, cacheControl.staleWhileRevalidateSeconds)
    }

    @Test
    fun `builder should truncate precision to seconds`() {
        val cacheControl = CacheControl.Builder()
            .maxAge(4999, TimeUnit.MILLISECONDS)
            .build()

        assertEquals(4, cacheControl.maxAgeSeconds)
    }

    @Test
    fun `parse should set correct values for cache control`() {
        val header = "no-cache, no-store, max-age=1, private, must-revalidate, max-stale=2, min-fresh=3, " +
            "stale-if-error=4, stale-while-revalidate=5, only-if-cached, no-transform, immutable"
        val headers = Headers.of(
            "Cache-Control", "private, community=\"UCI\"",
            "Cache-Control", header
        )

        val cacheControl = CacheControl.parse(headers)
        assertTrue(cacheControl.noCache)
        assertTrue(cacheControl.noStore)
        assertTrue(cacheControl.isPrivate)
        assertTrue(cacheControl.immutable)
        assertTrue(cacheControl.noTransform)
        assertTrue(cacheControl.onlyIfCached)
        assertTrue(cacheControl.mustRevalidate)
        assertFalse(cacheControl.isPublic)
        assertEquals(1, cacheControl.maxAgeSeconds)
        assertEquals(2, cacheControl.maxStaleSeconds)
        assertEquals(3, cacheControl.minFreshSeconds)
        assertEquals(4, cacheControl.staleIfErrorSeconds)
        assertEquals(5, cacheControl.staleWhileRevalidateSeconds)
    }

    @Test
    fun `parse should combine cache control with pragma`() {
        val headers = Headers.of(
            "Cache-Control", "max-age=1",
            "Pragma", "must-revalidate",
            "Pragma", "public"
        )

        val cacheControl = CacheControl.parse(headers)
        assertEquals("max-age=1, must-revalidate, public", cacheControl.toString())
    }
}
