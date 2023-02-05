package ru.yandex.telepathy.repository

import android.content.Context
import android.content.SharedPreferences
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import java.util.concurrent.TimeUnit

class RemoteConfigCacheTest {

    private val cacheTtlMs = TimeUnit.SECONDS.toMillis(10)

    private lateinit var prefs: SharedPreferences

    private lateinit var cache: RemoteConfigCache

    @Before
    fun runBeforeEachTest() {
        prefs = Mockito.mock(SharedPreferences::class.java)
        val context = Mockito.mock(Context::class.java).stub {
            on { getSharedPreferences(any(), any()) } doReturn prefs
        }
        cache = RemoteConfigCache(context)
    }

    @Test
    fun isConfigOutdated_whenUpdatedRecently_isFalse() {
        runTtlTest(System.currentTimeMillis() - cacheTtlMs / 2, cacheTtlMs, false)
    }

    @Test
    fun isConfigOutdated_whenUpdatedInNearFuture_isTrue() {
        runTtlTest(System.currentTimeMillis() + cacheTtlMs / 2, cacheTtlMs, true)
    }

    @Test
    fun isConfigOutdated_whenUpdatedLongAgo_isTrue() {
        runTtlTest(System.currentTimeMillis() - cacheTtlMs * 2, cacheTtlMs, true)
    }

    @Test
    fun isConfigOutdated_whenUpdatedInFarFuture_isTrue() {
        runTtlTest(System.currentTimeMillis() + cacheTtlMs * 2, cacheTtlMs, true)
    }

    private fun runTtlTest(lastUpdateTimestamp: Long, cacheTtlMs: Long, expectedResult: Boolean) {
        prefs.stub {
            on { getLong(RemoteConfigCache.keyLastWriteTimestamp, 0) } doReturn lastUpdateTimestamp
        }
        assertThat(cache.isConfigOutdated(cacheTtlMs)).isEqualTo(expectedResult)
    }
}
