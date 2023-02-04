package ru.auto.ara.core

import ru.auto.ara.interactor.ILocationAutoDetectInteractor
import ru.auto.ara.interactor.LocationAutoDetectInteractor
import ru.auto.data.model.geo.SuggestGeoItem
import ru.auto.data.repository.IGeoRepository
import rx.Observable

class TestLocationAutoDetectInteractor(
    private val geoRepository: IGeoRepository,
    private val locationAutoDetectInteractor: ILocationAutoDetectInteractor = LocationAutoDetectInteractor(geoRepository)
) : ILocationAutoDetectInteractor by locationAutoDetectInteractor {

    override fun observeGeoItemDetection(): Observable<SuggestGeoItem> = Observable.just(SUGGEST_GEO_ITEM)

    companion object {
        private val SUGGEST_GEO_ITEM = SuggestGeoItem(
            id = "172",
            name = "Уфа",
            parentName = "Республика Башкортостан",
            geoRadiusSupport = true,
            radius = 200
        )
    }
}
