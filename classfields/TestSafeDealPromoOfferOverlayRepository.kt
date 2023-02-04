package ru.auto.ara.core.mocks_and_stubbs

import ru.auto.data.repository.ISafeDealPromoOfferOverlayRepository
import rx.Completable
import rx.Single

class TestSafeDealPromoOfferOverlayRepository : ISafeDealPromoOfferOverlayRepository {

    @Volatile
    var viewsCount: Int = 0

    override fun getViewsCount(): Single<Int> = Single.just(viewsCount)

    override fun setViewsCount(viewsCount: Int): Completable = Completable.fromAction {
        this.viewsCount = viewsCount
    }
}
