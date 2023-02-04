package ru.auto.data.provider

import io.qameta.allure.kotlin.junit4.AllureRunner
 import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
 import ru.auto.ara.RxTest
import rx.observers.TestSubscriber

/**
 * @author aleien on 2019-05-28.
 */
@RunWith(AllureRunner::class) class MemoryStackCacheProviderTest : RxTest() {
    private val cache = MemoryStackCacheProvider(TEST_CACHE_LIMIT)
    private val startingCache = listOf(FIRST_BADGE, SECOND_BADGE, THIRD_BADGE)

    @Before
    fun setup() {
        val addingSub = TestSubscriber<Any>()
        cache.cache(startingCache)
            .subscribe(addingSub)
    }

    @Test
    fun `when evicting not existing element should not throw error`() {
        val evictionSub = TestSubscriber<Any>()
        cache.evict("not existing string")
            .subscribe(evictionSub)

        evictionSub.assertNoErrors()
        evictionSub.assertCompleted()
    }

    @Test
    fun `when evicted, should return list without element`() {
        val evictionSub = TestSubscriber<Any>()
        cache.evict(FIRST_BADGE)
            .subscribe(evictionSub)

        evictionSub.assertNoErrors()
        evictionSub.assertCompleted()

        val cacheSub = TestSubscriber<List<String>>()
        cache.get()
            .subscribe(cacheSub)

        assertThat(cacheSub.onNextEvents[0])
            .doesNotContain(FIRST_BADGE)
    }

    @Test
    fun `when several elements added, get() should return in reversed order`() {
        val cacheSub = TestSubscriber<List<String>>()
        cache.get()
            .subscribe(cacheSub)

        assertThat(cacheSub.onNextEvents[0])
            .containsExactlyElementsOf(startingCache.reversed())
    }

    @Test
    fun `when several elements added,should return them reversed order`() {
        val cacheSub = TestSubscriber<List<String>>()
        cache.cache(startingCache)
            .subscribe(cacheSub)

        assertThat(cacheSub.onNextEvents[0])
            .containsExactlyElementsOf(startingCache.reversed())
    }

    @Test
    fun `when several elements added by one, should return them in reversed order`() {
        val evictionSub = TestSubscriber<Any>()
        cache.clearAll()
            .subscribe(evictionSub)

        for (item in startingCache) {
            val addingSub = TestSubscriber<Any>()
            cache.cache(item)
                .subscribe(addingSub)
        }

        val cacheSub = TestSubscriber<List<String>>()
        cache.get()
            .subscribe(cacheSub)

        assertThat(cacheSub.onNextEvents[0])
            .containsExactlyElementsOf(startingCache.reversed())
    }

    @Test
    fun `when added more than max cache size, should remove oldest`() {
        val addingSub = TestSubscriber<Any>()
        cache.cache(FORTH_BADGE)
            .subscribe(addingSub)

        val cacheSub = TestSubscriber<List<String>>()
        cache.get()
            .subscribe(cacheSub)

        assertThat(cacheSub.onNextEvents[0])
            .containsExactly(FORTH_BADGE, THIRD_BADGE, SECOND_BADGE)
    }

    @Test
    fun `when added new element, it should be first`() {
        val addingSub = TestSubscriber<Any>()
        cache.cache(FORTH_BADGE)
            .subscribe(addingSub)

        val cacheSub = TestSubscriber<List<String>>()
        cache.get()
            .subscribe(cacheSub)

        assertThat(cacheSub.onNextEvents[0])
            .startsWith(FORTH_BADGE)
    }

    companion object {
        private const val TEST_CACHE_LIMIT = 3
        private const val FIRST_BADGE = "first badge"
        private const val SECOND_BADGE = "second badge"
        private const val THIRD_BADGE = "third badge"
        private const val FORTH_BADGE = "forth badge"
    }

}
