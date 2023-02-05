package ru.yandex.yandexnavi.projected.testapp.impl

import com.yandex.mapkit.GeoObject
import com.yandex.mapkit.geometry.Point
import com.yandex.navikit.GeoObjectUtils
import com.yandex.navikit.providers.bookmarks.BookmarkInfo
import com.yandex.navikit.providers.bookmarks.BookmarksCollection
import com.yandex.navikit.providers.bookmarks.BookmarksProvider
import ru.yandex.yandexnavi.projected.testapp.impl.KnownPoints.ADDR_YNDX_AURORA
import ru.yandex.yandexnavi.projected.testapp.impl.KnownPoints.ADDR_YNDX_RED_ROSE
import ru.yandex.yandexnavi.projected.testapp.impl.KnownPoints.YNDX_AURORA
import ru.yandex.yandexnavi.projected.testapp.impl.KnownPoints.YNDX_CITY
import ru.yandex.yandexnavi.projected.testapp.impl.KnownPoints.YNDX_LOTTE
import ru.yandex.yandexnavi.projected.testapp.impl.KnownPoints.YNDX_PITER
import ru.yandex.yandexnavi.projected.testapp.impl.KnownPoints.YNDX_RED_ROSE
import ru.yandex.yandexnavi.projected.testapp.impl.KnownPoints.YNDX_SAMARA
import ru.yandex.yandexnavi.projected.testapp.impl.KnownPoints.YNDX_TULA

class BookmarksProviderImpl : BookmarksProvider {

    private var counter = 0

    private val collection0 = mutableListOf<BookmarksCollection>()

    private val collection1 = mutableListOf(
        BookmarksCollection(
            "Единственная коллекция",
            mutableListOf(
                createBookmarkInfo("Дача", 53.530973, 49.425085),
                createBookmarkInfo("МФЦ", 54.193394, 37.577327),
                createBookmarkInfo("Гоша", 56.124024, 40.434069),
                createBookmarkInfo("Заправка по дороге", 56.320692, 43.924030)
            ),
            false
        )
    )

    private val collection4 = mutableListOf(
        BookmarksCollection(
            "Избранное",
            mutableListOf(
                createBookmarkInfo("Дача", 54.621128, 39.700568),
                createBookmarkInfo("МФЦ", 53.504989, 50.136239),
                createBookmarkInfo("Гоша", 52.975923, 49.712585),
                createBookmarkInfo("Заправка по дороге", 53.186162, 44.951967)
            ),
            false
        ),
        BookmarksCollection(
            "Топ кафешек",
            mutableListOf(
                createBookmarkInfo("Fake pizza"),
                createBookmarkInfo("Bar rentgen"),
                createBookmarkInfo("Destination unknown")
            ),
            false
        ),
        BookmarksCollection(
            "Unseen",
            mutableListOf(createBookmarkInfo("Fake")),
            false
        ),
        BookmarksCollection(
            "Empty",
            mutableListOf(),
            false
        )
    )

    private val collection7 = mutableListOf(
        BookmarksCollection(
            "0",
            mutableListOf(createBookmarkInfo("Fake")),
            false
        ),
        BookmarksCollection(
            "1",
            mutableListOf(createBookmarkInfo("Fake")),
            false
        ),
        BookmarksCollection(
            "2",
            mutableListOf(createBookmarkInfo("Fake")),
            false
        ),
        BookmarksCollection(
            "3",
            mutableListOf(createBookmarkInfo("Fake")),
            false
        ),
        BookmarksCollection(
            "4",
            mutableListOf(createBookmarkInfo("Fake")),
            false
        ),
        BookmarksCollection(
            "5",
            mutableListOf(createBookmarkInfo("Fake")),
            false
        ),
        BookmarksCollection(
            "6",
            mutableListOf(createBookmarkInfo("Fake")),
            false
        )
    )

    private val yandexCollection = yandexOfficesGeoObjects()
        .map {
            BookmarkInfo(
                it.name!!,
                GeoObjectUtils.getArrivalPosition(it),
                GeoObjectUtils.getAddress(it),
                null
            )
        }
        .let {
            BookmarksCollection(
                "Офисы яндекса 2021",
                it,
                false
            )
        }
        .let { mutableListOf(it) }

    private val collections = listOf(
        yandexCollection,
        collection7,
        collection4,
        collection1,
        collection0
    )

    override fun bookmarksCollections(): MutableList<BookmarksCollection> {
        return collections[counter++ % collections.size]
    }

    private fun createBookmarkInfo(title: String, lat: Double = 55.756028, lon: Double = 37.614229): BookmarkInfo {
        return BookmarkInfo(title, Point(lat, lon), "Nothing", null)
    }

    private fun yandexOfficesGeoObjects(): Array<GeoObject> {
        return arrayOf(
            GeoObjectUtils.createGeoObject("🏴‍☠️ Питер – Яндекс", "Санкт-Петербург,  Пискаревский проспект, 2, корп. 2", YNDX_PITER),
            GeoObjectUtils.createGeoObject("🔫 Тула – Яндекс", "Тула, ул. Советская, 67", YNDX_TULA),
            GeoObjectUtils.createGeoObject("🌹 Красная роза – Яндекс", ADDR_YNDX_RED_ROSE, YNDX_RED_ROSE),
            GeoObjectUtils.createGeoObject("⚓️ Аврора – Яндекс", ADDR_YNDX_AURORA, YNDX_AURORA),
            GeoObjectUtils.createGeoObject("🎱 Лотте – Яндекс", "г. Москва, Новинский бул., 8, стр. 2", YNDX_LOTTE),
            GeoObjectUtils.createGeoObject("👀 Сити – Яндекс", "г. Москва, 1-й Красногвардейский пр-д, 21, стр.1", YNDX_CITY),
            GeoObjectUtils.createGeoObject("Samara Яндекс", "Самара, Мичурина, 78", YNDX_SAMARA)
        )
    }
}
