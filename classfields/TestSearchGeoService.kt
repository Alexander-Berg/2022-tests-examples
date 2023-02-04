package ru.auto.ara.core

import ru.auto.data.model.LocationPoint
import ru.auto.feature.maps.mapkit.ISearchGeoService
import ru.auto.feature.maps.mapkit.SearchGeoService
import ru.auto.feature.maps.suggest.model.LocationSuggestModel
import rx.Single

class TestSearchGeoService(
    private val searchGeoService: ISearchGeoService = SearchGeoService()
) : ISearchGeoService by searchGeoService {

    override fun searchAddress(location: LocationPoint, zoom: Int): Single<String?> = if (location == DEFAULT_LOCATION_POINT) {
        Single.fromCallable { DEFAULT_ADDRESS }
    } else {
        searchGeoService.searchAddress(location, zoom)
    }

    override fun locationSuggest(location: LocationPoint, zoom: Int): Single<List<LocationSuggestModel>?> = Single.fromCallable {
        listOf(
            LocationSuggestModel("Яндекс", "Садовническая ул., 82, стр. 2", LocationPoint(55.734839, 37.642777)),
            LocationSuggestModel("Auto.ru", "Садовническая ул., 82, стр. 2", LocationPoint(55.735496, 37.642452)),
            LocationSuggestModel("Яндекс.Деньги", "Садовническая ул., 82, стр. 2", LocationPoint(55.734936, 37.642584)),
            LocationSuggestModel("Кофемания", "Садовническая ул., 82, стр. 2", LocationPoint(55.73507, 37.642492)),
            LocationSuggestModel("Райффайзенбанк", "Садовническая ул., 82, стр. 2", LocationPoint(55.734583, 37.642501)),
            LocationSuggestModel("Deutsche Bank", "Садовническая ул., 82, стр. 2", LocationPoint(55.735924, 37.642395)),
            LocationSuggestModel("Correas", "Садовническая ул., 82, стр. 2", LocationPoint(55.734837, 37.642479)),
            LocationSuggestModel("Paul", "Садовническая ул., 82, стр. 2", LocationPoint(55.735743, 37.64249))
        )
    }

    override fun locationSuggest(
        query: String,
        defaultLocation: LocationPoint
    ): Single<List<LocationSuggestModel>?> = Single.fromCallable {
        listOf(
            LocationSuggestModel("Аврора", "Бизнес-центр · Москва, Садовническая набережная, 79", null),
            LocationSuggestModel("Avrora-bc.ru", "Бизнес-центр · Москва, Садовническая набережная, 79", null),
            LocationSuggestModel("Аврора", "Бизнес-центр · Москва, Алтуфьевское шоссе, 37с22", null),
            LocationSuggestModel("Аврора", "Технопарк · Химки, Транспортный проезд, 2", null),
            LocationSuggestModel(
                "Кофемания",
                "Кофейня · Москва, Садовническая улица, 82с2",
                LocationPoint(0.0, 0.0)
            ),
            LocationSuggestModel("Садовническая набережная, 75", "Москва, Россия", null),
            LocationSuggestModel("Садовническая улица, 82с2", "Москва, Россия", null),
            LocationSuggestModel(
                "Аврора Недвижимость",
                "Агентство недвижимости · Москва, Большой Спасоглинищевский переулок, 9/1с10",
                null
            ),
            LocationSuggestModel("Аврора", "Логистическая компания · Москва, Алтуфьевское шоссе, 37с2", null)
        )
    }

    companion object {
        private val DEFAULT_LOCATION_POINT = LocationPoint(60.02457231535383, 30.31217721390641)
        private const val DEFAULT_ADDRESS = "Удельный проспект, 53"
    }
}
