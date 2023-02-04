package ru.auto.data.repository

import io.qameta.allure.kotlin.junit4.AllureRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.auto.ara.RxTest
import ru.auto.data.exception.DuplicateBadgeException
import ru.auto.data.provider.IBadgesListProvider
import ru.auto.data.provider.MemoryStackCacheProvider
import rx.Completable
import rx.Single
import rx.observers.TestSubscriber

/**
 * @author aleien on 2019-05-20.
 */
@RunWith(AllureRunner::class) class UserBadgesRepositoryTest : RxTest() {

    private val dictionaryBadgesProvider: IBadgesListProvider = mock<IBadgesListProvider>().apply {
        whenever(getBadges()).thenReturn(Single.just(DICTIONARY))
    }

    private val mockCache: MemoryStackCacheProvider = mock<MemoryStackCacheProvider>().apply {
        whenever(get()).thenReturn(Single.just(emptyList()))
        whenever(cache(any<String>())).thenReturn(Completable.complete())
        whenever(cache(any<List<String>>())).thenReturn(Single.just(emptyList()))
        whenever(evict(any())).thenReturn(Completable.complete())
    }

    private val repository: IUserBadgesRepository = UserBadgesRepository(
        dictionaryBadgesProvider,
        mockCache,
        mockCache
    )

    @Test
    fun `if badge exists in local badges, should throw exception`() {
        whenever(this.mockCache.cache(any<List<String>>())).thenReturn(Single.just(listOf(NEW_BADGE_LABEL)))
        val creationSub = TestSubscriber<Any>()
        repository.createUserBadge(NEW_BADGE_LABEL)
            .subscribe(creationSub)

        creationSub.assertError(DuplicateBadgeException::class.java)
    }

    @Test
    fun `if badge exists in local badges, should not create second`() {
        whenever(this.mockCache.get()).thenReturn(Single.just(listOf(NEW_BADGE_LABEL)))
        whenever(this.mockCache.cache(any<List<String>>())).thenReturn(Single.just(listOf(NEW_BADGE_LABEL)))

        val creationSub = TestSubscriber<Any>()
        repository.createUserBadge(NEW_BADGE_LABEL)
            .subscribe(creationSub)

        val badgesSub = TestSubscriber<List<String>>()
        repository.observeBadges()
            .subscribe(badgesSub)

        assertThat(badgesSub.onNextEvents[0])
            .containsOnlyOnce(NEW_BADGE_LABEL)
    }

    @Test
    fun `if badge exists in dictionary badges, should throw exception`() {
        val testSub = TestSubscriber<Any>()
        repository.createUserBadge(PARKTRONIK)
            .subscribe(testSub)

        testSub.assertError(DuplicateBadgeException::class.java)
    }

    @Test
    fun `if new badge is added, should notify listeners`() {
        val badgesSub = TestSubscriber<List<String>>()
        repository.observeBadges()
            .subscribe(badgesSub)

        val createSub = TestSubscriber<Any>()
        repository.createUserBadge(NEW_BADGE_LABEL)
            .subscribe(createSub)

        assertThat(badgesSub.onNextEvents[1])
            .contains(NEW_BADGE_LABEL)
    }

    @Test
    fun `after adding recent badge from dictionary, it should be first and not duplicate`() {
        val badgesSub = TestSubscriber<List<String>>()
        repository.observeBadges()
            .subscribe(badgesSub)

        whenever(this.mockCache.get()).thenReturn(Single.just(listOf(OTHER_STUFF)))

        val addingBadgeSub = TestSubscriber<Any>()
        repository.addRecentBadge(OTHER_STUFF)
            .subscribe(addingBadgeSub)

        assertThat(badgesSub.onNextEvents[1])
            .startsWith(OTHER_STUFF)
            .containsOnlyOnce(OTHER_STUFF)
            .containsAll(DICTIONARY)
            .size()
            .isEqualTo(DICTIONARY.size)
    }

    @Test
    fun `after adding custom recent badge, it should be first`() {
        val badgesSub = TestSubscriber<List<String>>()
        repository.observeBadges()
            .subscribe(badgesSub)

        whenever(this.mockCache.get()).thenReturn(Single.just(listOf(NEW_BADGE_LABEL)))

        val addingBadgeSub = TestSubscriber<Any>()
        repository.addRecentBadge(NEW_BADGE_LABEL)
            .subscribe(addingBadgeSub)

        assertThat(badgesSub.onNextEvents[1])
            .startsWith(NEW_BADGE_LABEL)
            .containsOnlyOnce(NEW_BADGE_LABEL)
            .containsAll(DICTIONARY)
            .size()
            .isEqualTo(DICTIONARY.size + 1)
    }

    companion object {
        private const val NEW_BADGE_LABEL = "Новый бейдж"

        private const val PARKTRONIK = "Парктроник"
        private const val BEST_PRICE = "Лучшая цена"
        private const val OTHER_STUFF = "Все такое"

        private val DICTIONARY = listOf(PARKTRONIK, BEST_PRICE, OTHER_STUFF)
    }

}
