package ru.auto.data.repository.favorite

import io.qameta.allure.kotlin.junit4.AllureRunner
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.auto.data.model.action.FavoriteOfferSyncAction
import ru.auto.data.model.action.SyncActionType
import ru.auto.testextension.completedWithNoErrors
import ru.auto.testextension.testWithSubscriber
import rx.Observable
import rx.Single
import rx.observers.TestSubscriber
import rx.schedulers.Schedulers
import java.io.IOException

/**
 * @author dumchev on 12.04.2018.
 */
@RunWith(AllureRunner::class)
 @Suppress("RemoveRedundantBackticks")
class FavoriteRepoTestImpl : FavoritesRepositoryTest() {

    private val exception = Exception()
    private val ioException = IOException("no internet connection")

    @Test
    fun `add | remove affect inner cache`() = with(favRepo) {
        testWithSubscriber(add(mockOffer).toSingle { Unit }) {
            check(idsCache.size == 1) {
                "add() doesn't affect cache size; actual idCache.size == ${idsCache.size}"
            }
        }
        testWithSubscriber(remove(mockOffer).toSingle { Unit }) {
            check(idsCache.isEmpty()) {
                "remove() doesn't affect cache size; actual idCache.size == ${idsCache.size}"
            }
        }
    }

    @Test
    fun `save to cache when add or remove favorite offline`() {
        // no connection
        whenever(api.addFavorite(any(), any())).thenReturn(Single.error(ioException))
        whenever(api.removeFavorite(any(), any())).thenReturn(Single.error(ioException))

        testWithSubscriber(favRepo.add(mockOffer).toSingle { Unit }) {
            it.awaitTerminalEvent()
            testWithSubscriber(favRepo.remove(mockOffer).andThen(syncStorage.get())) {
                it.awaitTerminalEvent()
                it.assertValue(emptyList()) // we should have add and remove, so 0 size
                println(syncStorage.get().toBlocking().value().size)
            }
        }
    }

    @Test
    fun `if we had saved without connection, next request should try to sync`() {
        // no connection
        whenever(api.addFavorite(any(), any())).thenReturn(Single.error(ioException))

        testWithSubscriber(favRepo.add(mockOffer).toSingle { Unit })  // this should save offer to local cache

        favRepo.sync().subscribe {
            // check, that we are trying to addFavorite on next request
            verify(api, times(2)).addFavorite(carCategory, offerId)
        }
    }

    @Test
    fun `check that we can get favorites even when add|remove request throw error`() {
        whenever(api.addFavorite(any(), any())).thenReturn(Single.error(exception))

        // we add here to have not-sync cache; after that observeFavorites() will trigger sync
        favRepo.add(mockOffer).subscribe()

        testWithSubscriber(favRepo.sync()) { it.assertNoErrors() }
    }

    @Test
    fun `add with error and then remove without error — will have empty cache`() {
        // can't add and try to add
        whenever(api.addFavorite(any(), any())).thenReturn(Single.error(exception))
        testWithSubscriber(favRepo.add(mockOffer).toSingle { Unit }) { it.awaitTerminalEvent() }

        // now we can add and try to remove (successfully)
        whenever(api.addFavorite(any(), any())).thenReturn(Single.just(successResponse))
        testWithSubscriber(favRepo.remove(mockOffer).toSingle { Unit }) { it.awaitTerminalEvent() }

        // now we should have no cache and should have no local sync actions
        testWithSubscriber(syncStorage.get()) { sub ->
            sub.assertValue(kotlin.collections.emptyList())
            check(favRepo.hasCache.not()) { "now we should have no cache; actual size ${favRepo.idsCache.size}" }
        }
    }


    @Test
    fun `no concurrency modification exception`() {

        fun runConcurrencyIssuePossibility() {
            // if we have cached, we will send it in parallel
            Observable.from(1..10).flatMapSingle({ i ->
                syncStorage.add(FavoriteOfferSyncAction(offerId + i, SyncActionType.ADD, carCategory))
            }).toBlocking().subscribe()

            val single = favRepo.sync().subscribeOn(Schedulers.newThread())
            testWithSubscriber(single)
        }

        (1..50).forEach { runConcurrencyIssuePossibility() }
    }

    @Test
    fun `do not have same items in cache`() {
        whenever(api.addFavorite(any(), any())).thenReturn(Single.error(ioException))
        testWithSubscriber(favRepo.add(mockOffer).toSingle { Unit })
        testWithSubscriber(favRepo.add(mockOffer).toSingle { Unit })

        val testSub = TestSubscriber<List<FavoriteOfferSyncAction>>()
        check(favRepo.idsCache.size == 1) { "we should have cache == 1, after we add the same offer 2 times" }
        syncStorage.get()
                .doOnSuccess { items -> check(items.size == 1, { "we should have 1 item in cache" }) }
                .subscribe(testSub)
        testSub.completedWithNoErrors()
    }

    @Test
    fun `add, remove, sync affect getAll`() {

        testWithSubscriber(
                favRepo.sync().toCompletable()
                        .andThen(favRepo.getAll().take(1).toSingle())
        ) { testSubscriber ->
            testSubscriber.assertNoErrors()
        }

        favRepo.add(mockOffer).subscribe()
        testWithSubscriber(favRepo.getAll().take(1).toSingle()) { testSubscriber ->
            testSubscriber.assertValue(listOf(mockOffer))
        }

        favRepo.remove(mockOffer).subscribe()
        testWithSubscriber(favRepo.getAll().take(1).toSingle()) { testSubscriber ->
            testSubscriber.assertValue(listOf())
        }
    }

    @Test
    fun `can't have duplicates in repo`() {
        favRepo.add(mockOffer).andThen(favRepo.add(mockOffer)).subscribe()
        testWithSubscriber(favRepo.getAll().take(1).toSingle()) { testSubscriber ->
            testSubscriber.assertValue(listOf(mockOffer))
        }
    }
}
