package ru.auto.ara.core

import ru.auto.data.model.geo.SuggestGeoItem
import ru.auto.data.repository.IGeoRepository
import rx.Completable
import rx.Single

class TestGeoRepository(
    private val args: GeoArgs
) : IGeoRepository {
    override fun getRadiusNow(): Int? = args.radius

    override fun getRadius(): Single<Int?> = Single.just(getRadiusNow())

    override fun saveRadius(radius: Int?): Completable = Completable.complete()

    override fun saveSelectedRegions(items: List<SuggestGeoItem>): Completable = Completable.complete()

    override fun getFirstSelected(): Single<SuggestGeoItem?> = Single.just(args.regions.firstOrNull())

    override fun getSelectedRegions(): Single<List<SuggestGeoItem>> = Single.just(args.regions)

    override fun getSelectedRegionsNow(): List<SuggestGeoItem> = args.regions

    override fun updateCache(newVersion: String): Completable = Completable.complete()

    override fun getAllRegions(): Single<List<SuggestGeoItem>> = Single.just(listOf())

    override fun getGeoSuggest(
        isOnlyCities: Boolean,
        lat: Double?,
        lon: Double?,
        letters: String?,
    ): Single<List<SuggestGeoItem>> = Single.just(args.regions)

    override fun getGeoSuggest(geoId: String): Single<SuggestGeoItem?> = Single.just(SuggestGeoItem(geoId, "Заглушки"))

    class GeoArgs(
        var regions: List<SuggestGeoItem>,
        var radius: Int?
    ) {
        companion object {
            fun moscow300(geoRadiusSupport: Boolean = false): GeoArgs = GeoArgs(
                listOf(SuggestGeoItem("213", "Москва", geoRadiusSupport = geoRadiusSupport)),
                300
            )
            fun ufa300(geoRadiusSupport: Boolean = false): GeoArgs = GeoArgs(
                listOf(SuggestGeoItem("172", "Уфа", geoRadiusSupport = geoRadiusSupport)),
                300
            )
        }
    }
}
