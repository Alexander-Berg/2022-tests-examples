package com.yandex.maps.testapp.search.test

import com.yandex.mapkit.GeoObject
import com.yandex.mapkit.GeoObjectCollection
import com.yandex.mapkit.geometry.BoundingBox
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polyline
import com.yandex.mapkit.search.*
import com.yandex.mapkit.search.FilterCollectionUtils.createFilterCollectionBuilder
import com.yandex.mapkit.uri.UriObjectMetadata
import com.yandex.maps.testapp.search.*
import java.text.SimpleDateFormat
import java.util.*

const val ADVERT_MENU_PAGE_ID = "navi_menu_icon_1"
const val BILLBOARD_ADVERT_PAGE_ID = "maps_search_test"
const val GEOPRODUCT_PAGE_ID = "navi"
const val DIRECT_PAGE_ID = "3897"
const val MENU_PAGE_ID = "mobile_maps_search"

data class Place(val name: String,
            val region: Array<Int>,
            val point: Point,
            val route: Polyline? = null)

val MOSCOW_NAMES = arrayOf("Москва", "Moscow", "Moskova")
val MSK = Place("Moscow", arrayOf(1, 213), Point(55.74, 37.63),
        Polyline(listOf(Point(55.756888, 37.615071), Point(55.764656, 37.605244))))

val SPB = Place("Saint Petersburg", arrayOf(10174, 2), Point(59.95, 30.30),
        Polyline(listOf(Point(59.937128, 30.312444), Point(59.931011, 30.361025))))

val KIEV = Place("Kiev", arrayOf(143), Point(50.45, 30.52))

class AdvertMenuTest(runIf: Boolean, val point: Point)
    : TestCase("Advert menu", !runIf), AdvertMenuListener
{
    private var advertMenuManager = SearchFactory.getInstance().createAdvertMenuManager(ADVERT_MENU_PAGE_ID)

    override fun doTest() {
        advertMenuManager.addListener(this)
        advertMenuManager.setPosition(point)
    }

    override fun onMenuAdvertReceived() {
        checkThat(advertMenuManager.advertMenuInfo.menuItems.size, GreaterThan(0), "menuAdverts.count")
        finish()
    }
}


class BillboardRouteTest(runIf: Boolean, val route: Polyline?, private val routeManager: BillboardRouteManager)
    : TestCase("Billboard route", !runIf), AdvertRouteListener
{
    override fun doTest() {
        routeManager.addListener(this)
        routeManager.setRoute(route!!)
    }

    override fun onRouteAdvertReceived() {
        checkThat(routeManager.advertObjects.size, GreaterThan(0), "routeAdverts.count")
        routeManager.removeListener(this)
        finish()
    }
}

class MenuTest(runIf: Boolean, val point: Point)
    : TestCase("Menu", !runIf), MenuListener
{
    private var menuManager = SearchFactory.getInstance().createMenuManager(MENU_PAGE_ID)

    override fun doTest() {
        menuManager.addListener(this)
        menuManager.setPosition(point)
    }

    override fun onMenuReceived() {
        checkThat(menuManager.menuInfo.menuItems.size, GreaterThan(0), "menu.count")
        finish()
    }
}

typealias TestFun = () -> Unit

class SuiteTest(name: String,
                skip: Boolean,
                private val suite: SearchTestSuite,
                private val testFun: TestFun)
    : TestCase(name, skip)
{
    override fun doTest() {
        suite.test = this
        testFun()
    }
}


class SearchTestSuite {
    companion object {
        private val TOPONYM_POINT = Point(55.733670, 37.587874)
        private val YANDEX_POINT = Point(55.733915, 37.588335)
        private val EUROPE_TC_POINT = Point(55.744813, 37.566099)

        private const val ZOOM = 11
        private const val RESULTS = 10

        private val commonOptions: SearchOptions
            get() = makeSearchOptions(SearchType.GEO.value or SearchType.BIZ.value, null)
                .setResultPageSize(RESULTS)

        private val companyOptions = makeSearchOptions(SearchType.BIZ.value, null)
            .setSnippets(Snippet.BUSINESS_RATING1X.value or
                Snippet.MASS_TRANSIT.value or
                Snippet.PANORAMAS.value or
                Snippet.PHOTOS.value or
                Snippet.PHOTOS3X.value or
                Snippet.SUBTITLE.value
            )

        private fun <T> suffix(list: List<T>, length: Int) = list.drop(list.size - length)
    }

    var test: TestCase? = null
    private fun <T> checkThat(value: T?, predicate: Predicate<T>, msg: String) {
        test!!.checkThat(value, predicate, msg)
    }
    private fun checkThat(value: Boolean, msg: String) = checkThat(value, Is(true), msg)

    private var searchManager: SearchManager? = null
    private val billboardRouteManager: BillboardRouteManager by lazy {
        SearchFactory.getInstance().createBillboardRouteManager(BILLBOARD_ADVERT_PAGE_ID)
    }

    // test shared state
    private var session: Session? = null
    private var goodsRegisterSession: GoodsRegisterSession? = null
    private var bookingSearchSession: BookingSearchSession? = null
    private var savedNames: List<String>? = null
    private var savedPoints: List<Point>? = null
    private var savedGoodsUri: String? = null
    private var savedBookingUri: String? = null
    private var savedCollectionUri: String? = null
    private var pharmacyResultCount: Int? = null

    private var isOnline: Boolean = true
    private var isRussianLocale: Boolean = true

    fun setUp(runner: TestRunner, isOnline: Boolean = true, isRussianLocale: Boolean = false) {
        this.isOnline = isOnline
        this.isRussianLocale = isRussianLocale
        searchManager = SearchFactory.getInstance().createSearchManager(
            if (isOnline) SearchManagerType.ONLINE else SearchManagerType.OFFLINE
        )
        generateTests().forEach{ runner.addTest(it) }
    }

    private fun generateTests(): List<TestCase> {
        val result = mutableListOf(
            makeTest("Requesting company at ${toString(YANDEX_POINT)}") { testReverseCompanyCard() },
            makeTest("Requesting company by URI") { testCompanyCardByURI() },
            makeTest("Requesting advertised companies", runIf = isOnline) { testAdvertisements() },
            makeTest("Requesting direct objects", runIf = isOnline && isRussianLocale) { testDirectObjects() },

            makeTest("Requesting toponym 'Moscow, Lva Tolstogo'") { testToponymStreetCard() },
            makeTest("Requesting toponym 'Moscow, Lva Tolstogo, 16'") { testToponymHouseCard() },
            makeTest("Requesting toponym at ${toString(TOPONYM_POINT)} without zoom") { testReverseSearchWithoutZoom() },
            makeTest("Requesting toponym at ${toString(TOPONYM_POINT)} with zoom $ZOOM", runIf = isOnline) { testReverseSearchWithZoom() },
            makeTest("Requesting toponym with former name", runIf = isOnline) { testFormerName() },

            makeTest("Requesting transit card", runIf = isOnline) { testTransitCard() },

            makeTest("Requesting experimental snippet", runIf = isOnline) { testExperimentalSnippet() },

            makeTest("Requesting companies at SEC \"EUROPEISKIY\"") { testReverseFirstPage() },
            makeTest("Requesting companies next page") { testNextPage() },

            makeTest("Requesting goods/1.x snippet", runIf = isOnline) { testGoods1xSnippet() },
            makeTest("Requesting goods register", runIf = isOnline) { testGoodsRegister() },

            makeTest("Requesting yandex_travel/1.x snippet", runIf = isOnline) { testYandexTravelSnippet() },
            makeTest("Requesting booking", runIf = isOnline) { testBookingSearch() },

            makeTest("Requesting fuel snippet", runIf = isOnline) { testFuelSnippet() },

            makeTest("Requesting visual hints snippet", runIf = isOnline) { testVisualHintsSnippet() },

            makeTest("Requesting encyclopedia snippet", runIf = isOnline) { testEncyclopediaSnippet() },

            makeTest("Searching collections", runIf = isOnline) { testCollectionCard() },
            makeTest("Requesting collection by URI", runIf = isOnline) { testCollectionByURI() },

            makeTest("Searching with a typo", runIf = isOnline) { testCorrectedMisspell() },
            makeTest("Searching with a typo. Request correction is disabled", runIf = isOnline) {
                testNotCorrectedMisspell()
            }
        )

        for (place in listOf(MSK, SPB, KIEV)) {
            result.addAll(makeCommonTests(place))
        }

        if (isOnline) {
            result.addAll(makeBannerTests())
        }

        return result
    }

    private fun makeCommonTests(place: Place): List<TestCase> {
        val point = place.point
        val dl = 0.02
        val points = arrayOf(
            point.shift(-dl, -dl),
            point.shift(-dl, dl),
            point.shift(dl, dl),
            point.shift(dl, -dl),
            point.shift(-dl, -dl)
        )
        val span = makeBoundingBox(point, dl)
        val shouldTestAdverts = isOnline && place != KIEV

        return listOf(
            makeTest("\n--- ${place.name} ---") { testFirstPage(point) },
            makeTest("Next page") { testNextPage() },

            makeTest("Default sort") { testDefaultSort(point) },
            makeTest("Sort by distance from point") { testSortFromPoint(point) },
            makeTest("Sort by distance from route (~fifty points)", runIf = isOnline) {
                testSortFromRoute(makeRoute(points, 10))
            },
            makeTest("Sort by distance from route (~thousand points)", runIf = isOnline) {
                testSortFromRoute(makeRoute(points, 200))
            },
            makeTest("Sort by distance from route. Primary request (~fifty points)", runIf = isOnline) {
                testSortFromRoutePrimaryRequest(makeRoute(points, 10))
            },
            makeTest("Sort by distance from route. Primary request (~thousand points)", runIf = isOnline) {
                testSortFromRoutePrimaryRequest(makeRoute(points, 200))
            },
            makeTest("First page") { testUnfiltered(point) },
            makeTest("Filtered page (basic)") { testFilteredBasic() },
            makeTest("Filtered page (range)", runIf = isOnline) { testFilteredRange() },
            makeTest("Filtered page (primary request)", runIf = isOnline) {
                testFilteredPrimaryRequest(point)
            },
            makeTest("Filtered page (date)", runIf = isOnline) {
                testFilteredDate(point)
            },

            makeTest("Span search") { testSpanSearch(span) },
            makeTest("Drag search") { testDragSearch(span, 2 * dl) },

            MenuTest(isOnline, place.point),
            AdvertMenuTest(shouldTestAdverts, place.point),
            BillboardRouteTest(shouldTestAdverts, place.route, billboardRouteManager)
        )
    }

    private fun makeTest(name: String, runIf: Boolean = true, testFun: TestFun): TestCase {
        return SuiteTest(name, !runIf, this, testFun)
    }

    private abstract inner class TestSearchListener : Session.SearchListener {
        override fun onSearchResponse(response: Response) {
            try {
                onResponse(response)
            } catch (e: Exception) {
                test!!.fail("Exception in test: $e")
            }

            test!!.finish()
        }

        override fun onSearchError(error: com.yandex.runtime.Error) {
            test!!.fail("Search error :" + error.javaClass.name)
            test!!.finish()
        }

        abstract fun onResponse(response: Response)
    }

    private abstract inner class TestGoodsRegisterListener : GoodsRegisterSession.GoodsRegisterListener {
        override fun onGoodsRegisterResponse(goodsRegister: GoodsRegister) {
            try {
                onResponse(goodsRegister)
            } catch (e: Exception) {
                test!!.fail("Exception in test: $e")
            }
            test!!.finish()
        }

        override fun onGoodsRegisterError(error: com.yandex.runtime.Error) {
            test!!.fail("Search error :" + error.javaClass.name)
            test!!.finish()
        }

        abstract fun onResponse(goodsRegister: GoodsRegister)
    }

    private abstract inner class TestBookingListener : BookingSearchSession.BookingSearchListener {
        override fun onBookingSearchResponse(response: BookingResponse) {
            try {
                onResponse(response)
            } catch (e: Exception) {
                test!!.fail("Exception in test: $e")
            }
            test!!.finish()
        }

        override fun onBookingSearchError(error: com.yandex.runtime.Error) {
            test!!.fail("Search error :" + error.javaClass.name)
            test!!.finish()
        }

        abstract fun onResponse(response: BookingResponse)
    }

    // first/next page test
    private fun testFirstPage(point: Point) {
        val searchListener = object : TestSearchListener() {
            override fun onResponse(response: Response) {
                checkPrimaryResponse(response)
                val categories = response.metadata
                    .businessResultMetadata
                    ?.categories
                    ?: listOf()
                checkThat(categories, IsNotEmptyList(), "categories")
            }
        }
        session = searchManager!!.submit("кафе", point.asGeometry(), commonOptions, searchListener)
    }

    private fun testNextPage() {
        val searchListener = object : TestSearchListener() {
            override fun onResponse(response: Response) = checkSecondaryResponse(response)
        }
        if (session!!.hasNextPage()) {
            session!!.fetchNextPage(searchListener)
        } else {
            test!!.fail("hasNextPage() == false")
        }
    }

    private fun testReverseFirstPage() {
        val searchListener = object : TestSearchListener() {
            override fun onResponse(response: Response) = checkPrimaryResponse(response)
        }
        session = searchManager!!.submit(EUROPE_TC_POINT, null, companyOptions, searchListener)
    }

    // sorting tests
    private fun testDefaultSort(point: Point) {
        val searchListener = object : TestSearchListener() {
            override fun onResponse(response: Response) {
                checkPrimaryResponse(response)
                checkThat(response.metadata.sort?.type, IsNot(SortType.DISTANCE), "response sort type")
            }
        }
        val bbox = makeBoundingBox(point, 1.0)
        session = searchManager!!.submit("кафе", bbox.asGeometry(), commonOptions, searchListener)
    }

    private fun testSortFromPoint(regionPoint: Point) {
        // FIXME(bkuchin): use `regionPoint` again after GEOSEARCH-6127 is fixed
        val point = savedPoints
            ?.maxByOrNull { it.distances(savedPoints!!).average() }
            ?: regionPoint
        val searchListener = object : TestSearchListener() {
            override fun onResponse(response: Response) {
                checkSecondaryResponse(response)
                val metadata = response.metadata
                checkThat(metadata.sort?.type, Is(SortType.DISTANCE), "response sort type should be DISTANCE")
                val relevantObjectsDistances = point.distances(savedPoints!!)
                val nearestObjectsDistances = point.distances(response.points)
                checkThat(nearestObjectsDistances.average(),
                        LessThan(relevantObjectsDistances.average() / 2.0),
                        "sort from the point should affect average distance more")
            }
        }
        session!!.setSortByDistance(point.asGeometry())
        session!!.resubmit(searchListener)
    }

    private fun testSortFromRoute(route: Polyline) {
        val searchListener = object : TestSearchListener() {
            override fun onResponse(response: Response) {
                checkResponseOnSortByRoute(route, response)
            }
        }
        session!!.setSearchArea(route.asGeometry())
        session!!.setSortByDistance(route.asGeometry())
        session!!.resubmit(searchListener)
    }

    private fun testSortFromRoutePrimaryRequest(route: Polyline) {
        val searchListener = object : TestSearchListener() {
            override fun onResponse(response: Response) {
                checkResponseOnSortByRoute(route, response)
            }
        }
        session = searchManager!!.submit("кафе", route, route.asGeometry(), commonOptions, searchListener)
    }

    // filters test
    private fun testUnfiltered(point: Point) {
        val searchListener = object : TestSearchListener() {
            override fun onResponse(response: Response) {
                checkPrimaryResponse(response)
                checkFilter(response, "wi_fi", selected = false)
                checkFilter(response, "italian_cuisine", selected = false)
            }
        }
        session = searchManager!!.submit("кафе", makeBoundingBox(point, 0.5).asGeometry(),
            commonOptions, searchListener)
    }

    private fun testFilteredBasic() {
        val searchListener = object : TestSearchListener() {
            override fun onResponse(response: Response) {
                checkSecondaryResponse(response)
                checkFilter(response, "wi_fi", selected = true)
                checkFilter(response, "italian_cuisine", selected = true)
            }
        }

        session!!.setSearchOptions(commonOptions.setFilters(
            createFilterCollectionBuilder().run {
                addBooleanFilter("wi_fi")
                addEnumFilter("type_cuisine", listOf("italian_cuisine"))
                build()
            }
        ))
        session!!.resubmit(searchListener)
    }

    private fun testFilteredRange() {
        val threshold = 500
        val searchListener = object : TestSearchListener() {
            override fun onResponse(response: Response) {
                checkSecondaryResponse(response)
                val digits = Regex("\\d+")
                response.results.forEach { geoObject ->
                    val averageBill = geoObject
                        .metadata<BusinessObjectMetadata>()
                        ?.features
                        ?.find { it.id == "average_bill2" }
                        ?.value
                        ?.textValue
                        ?.firstOrNull()
                    val lowerBound = averageBill
                        ?.let { digits.find(it) }
                        ?.value
                        ?.toIntOrNull()
                    checkThat(
                        lowerBound,
                        LessOrEqualTo(threshold),
                        "$averageBill for ${geoObject.uri()} not in range"
                    )
                }
            }
        }

        session!!.setSearchOptions(commonOptions.setFilters(
            createFilterCollectionBuilder().run {
                addRangeFilter("average_bill2", 1.0, threshold.toDouble())
                build()
            }
        ))
        session!!.resubmit(searchListener)
    }

    private fun testFilteredPrimaryRequest(point: Point) {
        val threshold = "gt4.5"
        val searchListener = object : TestSearchListener() {
            override fun onResponse(response: Response) {
                checkThat(
                    response.collection.children,
                    IsNotEmptyList(),
                    "results should be not empty"
                )
                checkThat(
                    response.names,
                    NotEqualTo(savedNames),
                    "results should differ when filtered"
                )
                checkFilter(response, threshold, selected = true)
                response.results.forEach { geoObject ->
                    val rating = geoObject
                        .metadata<BusinessRating1xObjectMetadata>()
                        ?.score
                        ?.toDouble()
                    checkThat(
                        rating,
                        GreaterOrEqualTo(4.5),
                        "rating for ${geoObject.uri()} is less than expected"
                    )
                }
            }
        }
        session = searchManager!!.submit(
            "кафе",
            makeBoundingBox(point, 0.5).asGeometry(),
            commonOptions
                .setFilters(createFilterCollectionBuilder().run {
                    addEnumFilter("rating_threshold", listOf(threshold))
                    build()
                })
                .setSnippets(Snippet.BUSINESS_RATING1X.value)
            ,
            searchListener
        )
    }

    private fun testFilteredDate(point: Point) {
        val searchListener = object : TestSearchListener() {
            override fun onResponse(response: Response) {
                checkThat(
                    response.metadata.businessResultMetadata
                        ?.businessFilters
                        ?.find { it.id == "hotel_price_range" },
                    IsNotNull(),
                    "hotel_price_range filter should be available"
                )
            }
        }
        val format = SimpleDateFormat("yyyyMMdd")
        session = searchManager!!.submit(
            "гостиница",
            makeBoundingBox(point, 0.5).asGeometry(),
            commonOptions.setFilters(createFilterCollectionBuilder().run {
                addDateFilter(
                    "hotel_date_range",
                    Calendar.getInstance().run {
                        add(Calendar.DATE, 1)
                        format.format(time)
                    },
                    Calendar.getInstance().run {
                        add(Calendar.DATE, 2)
                        format.format(time)
                    }
                )
                build()
            }),
            searchListener
        )
    }

    // drag test
    private fun testSpanSearch(bbox: BoundingBox) {
        val searchListener = object : TestSearchListener() {
            override fun onResponse(response: Response) {
                checkPrimaryResponse(response)
                checkInside(response, bbox)
            }
        }
        session = searchManager!!.submit("кафе", bbox.asGeometry(), commonOptions, searchListener)
    }

    private fun testDragSearch(bbox: BoundingBox, delta: Double) {
        val bboxShifted = bbox.shift(delta, 0.0)

        val searchListener = object : TestSearchListener() {
            override fun onResponse(response: Response) {
                checkSecondaryResponse(response)
                checkOutside(response, bbox)
                checkInside(response, bboxShifted)
            }
        }
        session!!.setSearchArea(bboxShifted.asGeometry())
        session!!.resubmit(searchListener)
    }

    private fun testCompanyCardByURI() {
        session = searchManager!!.resolveURI("ymapsbm1://org?oid=1098695873", companyOptions,
            grabliChecker)
    }

    private fun testCollectionByURI() {
        val searchListener = object : TestSearchListener() {
            override fun onResponse(response: Response) {
                val metadata = response.metadata.collectionResultMetadata
                checkThat(metadata, IsNotNull(), "collectionResultMetadata")

                val collectionCard = metadata?.collection

                checkThat(collectionCard?.itemCount, GreaterThan(0), "empty collection")
                checkThat(collectionCard?.rubric, IsNotEmpty(), "rubric")
                checkThat(collectionCard?.author?.name, IsNotEmpty(), "author")
                checkThat(collectionCard?.title, IsNotEmpty(), "title")
                checkThat(collectionCard?.uri, Is(savedCollectionUri), "uri")

                checkThat(response.results.size, Is(collectionCard?.itemCount), "results size")
                response.results.forEach {
                    val entryMetadata = it.metadata<CollectionEntryMetadata>()
                    checkThat(entryMetadata, IsNotNull(), "entry metadata")
                    checkThat(entryMetadata?.title, IsNotEmpty(), "title")
                }
            }
        }

        val options = makeSearchOptions(null, ExtendedSearchType.COLLECTIONS.value).setResultPageSize(100)
        savedCollectionUri?.let {
            session = searchManager!!.searchByURI(it, options, searchListener)
        } ?: run {
            test!!.fail("empty collection uri")
            test!!.finish()
        }
    }

    private fun testReverseCompanyCard() {
        val searchListener = object : TestSearchListener() {
            override fun onResponse(response: Response) {
                val geoObject = response.results.find { it.name!!.contains("Яндекс") || it.name!!.contains("Yandex") }
                checkThat(geoObject, IsNotNull(), "'Яндекс' not found")
                if (geoObject == null)
                    return

                val metadata = geoObject.metadata<BusinessObjectMetadata>()!!

                checkAddress(metadata.address, listOf(
                    Address.Component.Kind.STREET to arrayOf("Льва Толстого", "Lva Tolstogo"),
                    Address.Component.Kind.HOUSE to arrayOf("16")))

                checkHasAddressComponents(geoObject)
                checkHasAttributions(geoObject)

                checkHasURI(geoObject)
                checkHasRating(geoObject)
                checkHasStops(geoObject)
                checkHasPanoramas(geoObject)
                checkHasPhotos(geoObject)
            }
        }
        session = searchManager!!.submit(YANDEX_POINT, null, companyOptions, searchListener)
    }

    private fun testAdvertisements() {
        val advertChecker = object : TestSearchListener() {
            override fun onResponse(response: Response) {
                val geoObject = response.collection.children[0].obj!!
                val businessMetadata = geoObject.metadata<BusinessObjectMetadata>()
                checkThat(businessMetadata, IsNotNull(), "Business metadata")
                checkThat(businessMetadata?.advertisement, IsNotNull(), "No advertisement")
            }
        }

        val options = makeSearchOptions(SearchType.BIZ.value, null)
            .setAdvertPageId(GEOPRODUCT_PAGE_ID)

        session = searchManager!!.submit(
            "где поесть",
            makeBoundingBox(MSK.point, 1.0).asGeometry(),
            options,
            advertChecker)
    }

    private fun testDirectObjects() {
        val directChecker = object : TestSearchListener() {
            override fun onResponse(response: Response) {
                var directObjectCount = 0
                response.collection.children
                    .mapNotNull { it.obj?.metadata<DirectObjectMetadata>() }
                    .forEach {
                        directObjectCount++

                        checkThat(it.title, IsNotEmpty(), "title")
                        checkThat(it.url, IsNotEmpty(), "url")
                        checkThat(it.text, IsNotEmpty(), "text")
                    }

                checkThat(directObjectCount, IsNot(0), "no direct objects")
            }
        }

        val options = makeSearchOptions(ALL_SEARCH_TYPES, null).setDirectPageId(DIRECT_PAGE_ID)
        session = searchManager!!.submit(
            "пластиковые окна",
            MSK.point.asGeometry(),
            options,
            directChecker)
    }

    private fun toponymChecker(
        expectedComponents: List<Pair<Address.Component.Kind, Array<String>>>,
        checkEntrances: Boolean = false): TestSearchListener {
        return object : TestSearchListener() {
            override fun onResponse(response: Response) {
                val geoObject = response.collection.children[0].obj!!
                checkHasURI(geoObject)
                checkAddress(geoObject.metadata<ToponymObjectMetadata>()!!.address, expectedComponents)

                if (checkEntrances) {
                    val metadata = geoObject.metadata<RoutePointMetadata>()
                    checkThat(metadata, IsNotNull(), "entrance metadata")
                    checkThat(metadata?.entrances, IsNotNull(), "entrances")
                    metadata?.entrances?.map {
                        checkThat(toString(it.point), IsNotEmpty(), "point")
                        checkThat(it.direction, IsNotNull(), "direction")
                        val azimuth = it.direction!!.azimuth
                        checkThat(azimuth >= -360.0 && azimuth <= 360.0,
                            "Azimuth is out of range: %.2f".format(azimuth))
                    }
                }
            }
        }
    }

    private fun testToponymStreetCard() {
        val expectedComponents = listOf(
            Address.Component.Kind.LOCALITY to MOSCOW_NAMES,
            Address.Component.Kind.STREET to arrayOf("Льва Толстого", "Lva Tolstogo")
        )
        session = searchManager!!.resolveURI(
            "ymapsbm1://geo?text=Moscow, Lva Tolstogo&ll=37.62,55.75&spn=1,1",
            makeSearchOptions(SearchType.GEO.value, null),
            toponymChecker(expectedComponents))
        session = searchManager!!.resolveURI(
            "ymapsbm1://geo?data=Cgc4MDU5OTQ4EkDQoNC%2B0YHRgdC40Y8sINCc0L7RgdC60LLQsCwg0YPQu9C40YbQsCDQm9GM0LLQsCDQotC%2B0LvRgdGC0L7Qs9C%2BIgoNF1kWQhWe715C",
            makeSearchOptions(SearchType.GEO.value, null),
            toponymChecker(expectedComponents))
    }

    private fun testToponymHouseCard() {
        val expectedComponents = listOf(
            Address.Component.Kind.LOCALITY to MOSCOW_NAMES,
            Address.Component.Kind.STREET to arrayOf("Льва Толстого", "Lva Tolstogo"),
            Address.Component.Kind.HOUSE to arrayOf("16")
        )

        session = searchManager!!.resolveURI(
            "ymapsbm1://geo?text=Moscow, Lva Tolstogo, 16&ll=37.62,55.75&spn=1,1",
            makeSearchOptions(SearchType.GEO.value, null).setSnippets(Snippet.ROUTE_POINT.value),
            toponymChecker(expectedComponents, isOnline))
        session = searchManager!!.resolveURI(
            "ymapsbm1://geo?data=Cgg1NjY5NzYyMRJE0KDQvtGB0YHQuNGPLCDQnNC%2B0YHQutCy0LAsINGD0LvQuNGG0LAg0JvRjNCy0LAg0KLQvtC70YHRgtC%2B0LPQviwgMTYiCg0XWRZCFZ7vXkI%3D",
            makeSearchOptions(SearchType.GEO.value, null).setSnippets(Snippet.ROUTE_POINT.value),
            toponymChecker(expectedComponents, isOnline))
    }

    private fun testReverseSearchWithoutZoom() {
        val expectedComponents = listOf(
            Address.Component.Kind.LOCALITY to MOSCOW_NAMES,
            Address.Component.Kind.STREET to arrayOf("Льва Толстого", "Lva Tolstogo"),
            Address.Component.Kind.HOUSE to arrayOf("16")
        )

        session = searchManager!!.submit(
            TOPONYM_POINT,
            null,
            makeSearchOptions(SearchType.GEO.value, null),
            toponymChecker(expectedComponents))
    }

    private fun testReverseSearchWithZoom() {
        val expectedComponents = listOf(
            Address.Component.Kind.LOCALITY to MOSCOW_NAMES,
            Address.Component.Kind.DISTRICT to arrayOf("Центральн", "Tsentraln"),
            Address.Component.Kind.DISTRICT to arrayOf("Хамовники", "Khamovniki")
        )

        session = searchManager!!.submit(
            TOPONYM_POINT,
            ZOOM,
            makeSearchOptions(SearchType.GEO.value, null),
            toponymChecker(expectedComponents))
    }

    private fun testFormerName() {
        val formerNameChecker = object : TestSearchListener() {
            override fun onResponse(response: Response) {
                val geoObject = response.collection.children[0].obj!!
                val geoMetadata = geoObject.metadata<ToponymObjectMetadata>()
                checkThat(geoMetadata, IsNotNull(), "toponym metadata")
                checkThat(geoMetadata?.formerName, IsNotEmpty(), "former name")
                checkThat(geoMetadata?.formerName,
                    ContainsAnyOf("Днепропетровск", "Dnipropetrovsk", "Дніпропетровськ"),
                    "former name")
            }
        }

        session = searchManager!!.submit(
            "мiсто Днепр",
            MSK.point.asGeometry(),
            makeSearchOptions(SearchType.GEO.value, null),
            formerNameChecker)
    }

    private fun testCollectionCard() {
        val searchListener = object : TestSearchListener() {
            override fun onResponse(response: Response) {
                checkThat(response.collection.children.isNotEmpty(), "empty results")
                savedCollectionUri = uri(response.collection.children.firstOrNull())
                response.collection.children.forEach {
                    val metadata = it.obj?.metadata<CollectionObjectMetadata>()
                    checkThat(metadata, IsNotNull(), "CollectionObjectMetadata")

                    val collectionObject = metadata?.collection
                    checkThat(collectionObject?.title, IsNotEmpty(), "title")
                    checkThat(collectionObject?.uri, IsNotEmpty(), "uri")
                }
            }
        }
        val options = makeSearchOptions(null, ExtendedSearchType.COLLECTIONS.value)
        session = searchManager!!.submit("где поесть", MSK.point.asGeometry(), options, searchListener)
    }

    private fun testTransitCard() {
        val searchListener = object : TestSearchListener() {
            override fun onResponse(response: Response) {
                val geoObject = response.collection.children[0].obj!!

                checkThat(geoObject.name, IsNotEmpty(), "name")
                checkThat(geoObject.descriptionText, IsNotEmpty(), "description")

                val metadata = geoObject.metadata<TransitObjectMetadata>()
                checkThat(metadata, IsNotNull(), "Transit metadata should present")
                checkThat(metadata?.routeId, IsNotEmpty(), "route Id")
                checkThat(metadata?.types, IsNotNull(), "route type")
            }
        }
        val options = makeSearchOptions(null, ExtendedSearchType.TRANSIT.value)
        options.origin = "mobile-maps-searchnearby-text"
        session = searchManager!!.submit("автобус 1", MSK.point.asGeometry(), options, searchListener)
    }

    private fun experimentalItems(geoObject: GeoObject?) = geoObject
        ?.metadata<ExperimentalMetadata>()
        ?.experimentalStorage
        ?.items

    private fun testExperimentalSnippet() {
        val snippet = "afisha_json/1.x"
        val searchListener = object : TestSearchListener() {
            override fun onResponse(response: Response) {
                val hasRequestedSnippet = response.collection.children
                    .flatMap { experimentalItems(it.obj) ?: listOf() }
                    .any { it.key == snippet }
                checkThat( hasRequestedSnippet, Is(true), snippet )
            }
        }
        val options = makeSearchOptions(SearchType.BIZ.value, null)
        options.experimentalSnippets = listOf(snippet)
        session = searchManager!!.submit("кинотеатр", MSK.point.asGeometry(), options, searchListener)
    }

    private fun uri(item: GeoObjectCollection.Item?) = item
        ?.obj?.metadata<UriObjectMetadata>()
        ?.uris?.get(0)?.value

    private fun hasGoods1xSnippet(geoObject: GeoObject): Boolean {
        val goods = geoObject.metadata<Goods1xObjectMetadata>()?.goods ?: return false
        return goods.isNotEmpty() && goods.all { it.name.isNotEmpty() }
    }

    private fun testGoods1xSnippet() {
        val searchListener = object : TestSearchListener() {
            override fun onResponse(response: Response) {
                val item = response.collection.children
                    .find { hasGoods1xSnippet(it.obj!!) }
                savedGoodsUri = uri(item)
                checkThat(item, IsNotNull(), "missing goods/1.x")
            }
        }
        val options = makeSearchOptions(SearchType.BIZ.value, null)
            .setSnippets(Snippet.GOODS1X.value)
            .setResultPageSize(100)
        session = searchManager!!.submit("кафе", MSK.point.asGeometry(), options, searchListener)
    }

    private fun hasFuelSnippet(geoObject: GeoObject): Boolean {
        val fuels = geoObject.metadata<FuelMetadata>()?.fuels ?: return false
        return fuels.isNotEmpty() && fuels.all { it.name?.isNotEmpty() ?: false}
    }

    private fun testFuelSnippet() {
        val searchListener = object : TestSearchListener() {
            override fun onResponse(response: Response) {
                val item = response.collection.children
                        .find { hasFuelSnippet(it.obj!!) }
                checkThat(item, IsNotNull(), "missing fuel")
            }
        }
        val options = makeSearchOptions(SearchType.BIZ.value, null)
                .setSnippets(Snippet.FUEL.value)
                .setResultPageSize(100)
        session = searchManager!!.submit("АЗС", MSK.point.asGeometry(), options, searchListener)
    }

    private fun testVisualHintsSnippet() {
        val searchListener = object : TestSearchListener() {
            override fun onResponse(response: Response) {
                val searchResults = response.collection.children
                val hints = response.collection.children
                    .mapNotNull { it.obj?.metadata<VisualHintsObjectMetadata>() }
                val withSerpHints = hints.find { it.serpHints != null }
                val withCardHints = hints.find { it.cardHints != null }
                checkThat(withSerpHints, IsNotNull(), "missing serp hints")
                checkThat(withCardHints, IsNotNull(), "missing card hints")
            }
        }
        val options = makeSearchOptions(SearchType.BIZ.value, null)
                .setSnippets(Snippet.VISUAL_HINTS.value)
                .setResultPageSize(100)
        session = searchManager!!.submit("кафе", MSK.point.asGeometry(), options, searchListener)
    }

    private fun testEncyclopediaSnippet() {
        val searchListener = object : TestSearchListener() {
            override fun onResponse(response: Response) {
                checkThat(
                    response.collection.children.map { it.obj }.any {
                        it?.metadata<EncyclopediaObjectMetadata>() != null
                    },
                    Is(true),
                    "missing encyclopedia snippet"
                )
            }
        }
        val options = makeSearchOptions(SearchType.GEO.value, null)
            .setSnippets(Snippet.ENCYCLOPEDIA.value)
        session = searchManager!!.submit("москва", MSK.point.asGeometry(), options, searchListener)
    }

    private fun testGoodsRegister() {
        val goodsRegisterListener = object : TestGoodsRegisterListener() {
            override fun onResponse(goodsRegister: GoodsRegister) {
                checkThat(goodsRegister.categories, IsNotEmptyList(), "categories list is empty")
                for (category in goodsRegister.categories) {
                    checkThat(category.goods, IsNotEmptyList(), "goods list is empty")
                    category.goods.forEach {
                        checkThat(it.name, IsNotEmpty(), "goods name is empty")
                    }
                }
            }
        }
        savedGoodsUri?.let {
            goodsRegisterSession = searchManager!!.requestGoodsRegister(it, goodsRegisterListener)
        } ?: run {
            test!!.fail("empty goods uri")
            test!!.finish()
        }
    }

    private fun hasYandexTravelSnippet(geoObject: GeoObject): Boolean {
        return experimentalItems(geoObject)?.any { it.key == "yandex_travel/1.x" } ?: false
    }

    private fun testYandexTravelSnippet() {
        val searchListener = object : TestSearchListener() {
            override fun onResponse(response: Response) {
                val item = response.collection.children
                    .find { hasYandexTravelSnippet(it.obj!!) }
                savedBookingUri = uri(item)
                checkThat(item, IsNotNull(), "missing yandex_travel/1.x snippet")
            }
        }
        val options = makeSearchOptions(SearchType.BIZ.value, null)
            .setResultPageSize(20)
        options.experimentalSnippets = listOf("yandex_travel/1.x")
        session = searchManager!!.submit("отель", MSK.point.asGeometry(), options, searchListener)
    }

    private fun testBookingSearch() {
        val bookingListener = object : TestBookingListener() {
            override fun onResponse(response: BookingResponse) {
                checkThat(response.params, IsNotNull(), "booking params are null")
                checkThat(response.offers, IsNotEmptyList(), "booking offers list is empty")
                response.offers.forEach {
                    checkThat(it.partnerName, IsNotEmpty(), "booking partner name is empty")
                }
            }
        }
        savedBookingUri?.let {
            bookingSearchSession = searchManager!!.findBookingOffers(it, null, bookingListener)
        } ?: run {
            test!!.fail("empty booking uri")
            test!!.finish()
        }
    }

    private fun testCorrectedMisspell() {
        val searchListener = object : TestSearchListener() {
            override fun onResponse(response: Response) {
                checkThat(response.metadata.correctedRequestText, Is("аптека"),
                    "request was not corrected")
                pharmacyResultCount = response.collection.children.size
            }
        }
        val options = makeSearchOptions(SearchType.BIZ.value,null)
        session = searchManager!!.submit("аптика", MSK.point.asGeometry(), options, searchListener)
    }

    private fun testNotCorrectedMisspell() {
        val searchListener = object : TestSearchListener() {
            override fun onResponse(response: Response) {
                checkThat(response.metadata.correctedRequestText, IsNull(),
                    "request was corrected")
                checkThat(pharmacyResultCount, IsNotNull(),
                    "pharmacy request results count is null")
                checkThat(response.results.size, IsNot(pharmacyResultCount), "results count")
            }
        }
        val options = makeSearchOptions(SearchType.BIZ.value, null).setDisableSpellingCorrection(true)
        session = searchManager!!.submit("аптика", MSK.point.asGeometry(), options, searchListener)
    }

    // Shared request checkers
    private fun checkPrimaryResponse(response: Response) {
        checkThat(response.results.size, EqualTo(RESULTS), "results count")
        savedNames = response.names
        savedPoints = response.points
    }

    private fun checkSecondaryResponse(response: Response) {
        val results = response.collection.children
        checkThat(results.size, EqualTo(RESULTS), "results count")
        checkThat(response.names, NotEqualTo(savedNames), "secondary results should differ")
    }

    private fun checkBusinessBasicFields(geoObject: GeoObject) {
        val metadata = geoObject.metadata<BusinessObjectMetadata>()
        checkThat(metadata, IsNotNull(), "Business metadata")
        checkThat(metadata?.features, IsNotEmptyList(), "features")
        if (isOnline)
            checkThat(metadata?.featureGroups, IsNotEmptyList(), "feature groups")
    }

    private fun checkHasAddressComponents(geoObject: GeoObject) {
        if (!isOnline)
            return
        val metadata = geoObject.metadata<BusinessObjectMetadata>()!!
        checkThat(metadata.address.components, IsNotEmptyList(), "address components")
    }

    private fun checkHasWorkingHours(geoObject: GeoObject) {
        val metadata = geoObject.metadata<BusinessObjectMetadata>()!!
        checkThat(metadata.workingHours, IsNotNull(), "working hours data")
        checkThat(metadata.workingHours?.availabilities, IsNotEmptyList(), "working hours")
        if (isOnline)
            checkThat(metadata.workingHours?.state, IsNotNull(), "working hours state")
    }

    private fun checkHasAttributions(geoObject: GeoObject) {
        if (!isOnline)
            return
        checkThat(geoObject.aref, IsNotEmptyList(), "attribution references")
        checkThat(geoObject.attributionMap.values, IsNotEmptyList(), "attributions")
    }

    private fun checkHasPhotos(geoObject: GeoObject) {
        if (!isOnline)
            return
        val photoMetadata = geoObject.metadata<BusinessPhotoObjectMetadata>()
        checkThat(photoMetadata, IsNotNull(), "photos metadata")
        checkThat(photoMetadata?.photos, IsNotEmptyList(), "photos")
        val photo3xMetadata = geoObject.metadata<BusinessPhoto3xObjectMetadata>()
        checkThat(photo3xMetadata, IsNotNull(), "photos3x metadata")
        checkThat(photo3xMetadata?.groups, IsNotEmptyList(), "photos3x groups")
        checkThat(photo3xMetadata?.groups?.get(0)?.photos, IsNotEmptyList(), "photos3x")
    }

    private fun checkHasPanoramas(geoObject: GeoObject) {
        if (!isOnline)
            return
        val panoramasMetadata = geoObject.metadata<PanoramasObjectMetadata>()
        checkThat(panoramasMetadata, IsNotNull(), "panoramas metadata")
        checkThat(panoramasMetadata?.panoramas, IsNotEmptyList(), "panoramas")
    }

    private fun checkHasStops(geoObject: GeoObject) {
        if (!isOnline)
            return
        val stopsMetadata = geoObject.metadata<MassTransit1xObjectMetadata>()
        checkThat(stopsMetadata, IsNotNull(), "stops metadata")
        checkThat(stopsMetadata?.stops, IsNotEmptyList(), "stops")
    }

    private fun checkHasRating(geoObject: GeoObject) {
        val rating1xMetadata = geoObject.metadata<BusinessRating1xObjectMetadata>()
        checkThat(rating1xMetadata, IsNotNull(), "rating_1x metadata")
    }

    private fun checkHasURI(geoObject: GeoObject) {
        val uriMetadata = geoObject.metadata<UriObjectMetadata>()
        checkThat(uriMetadata, IsNotNull(), "URI metadata")
        checkThat(uriMetadata?.uris, IsNotEmptyList(), "URIs")
    }

    private fun checkHasSubtitle(geoObject: GeoObject) {
        if (!isOnline)
            return
        val subtitleMetadata = geoObject.metadata<SubtitleMetadata>()
        checkThat(subtitleMetadata, IsNotNull(), "subtitle metadata")
        checkThat(subtitleMetadata?.subtitleItems, IsNotEmptyList(), "subtitle items")
        subtitleMetadata?.subtitleItems?.forEach {
            checkThat(it.text != null || it.properties.isNotEmpty(), "subtitle item has text or properties")
        }
    }

    private val grabliChecker = object : TestSearchListener() {
        override fun onResponse(response: Response) {
            val geoObject = response.collection.children[0].obj!!
            checkBusinessBasicFields(geoObject)

            val metadata = geoObject.metadata<BusinessObjectMetadata>()!!
            checkThat(geoObject.name, IsOneOf("Грабли", "Grabli", "Grably"), "Name")
            checkThat(metadata.address.formattedAddress,
                ContainsAnyOf("Пятницкая", "Pyatnitskaya"),
                "formattedAddress")

            checkHasAddressComponents(geoObject)
            checkThat(metadata.categories.first()?.categoryClass, EqualTo("restaurants"), "category")
            checkThat(metadata.links, IsNotEmptyList(), "links")

            checkHasWorkingHours(geoObject)
            checkHasAttributions(geoObject)

            checkHasURI(geoObject)
            checkHasRating(geoObject)
            checkHasStops(geoObject)
            checkHasPanoramas(geoObject)
            checkHasPhotos(geoObject)
            checkHasSubtitle(geoObject)
        }
    }

    private fun checkInside(response: Response, bbox: BoundingBox) {
        for (p in response.points) {
            checkThat(bbox.contains(p),"${toString(bbox)} contains ${toString(p)}")
        }
    }

    private fun checkOutside(response: Response, bbox: BoundingBox) {
        for (p in response.points) {
            checkThat(!bbox.contains(p),"${toString(bbox)} does not contain ${toString(p)}")
        }
    }

    private fun checkFilter(response: Response, filterId: String, selected: Boolean) {
        val value = response.businessFilters.find{ it.id == filterId }?.selected
        checkThat(value, EqualTo(selected), "invalid selected state of '$filterId'")
    }

    private fun checkAddress(address: Address,
                             expectedComponents: List<Pair<Address.Component.Kind, Array<String>>>) {
        if (!isOnline)
            return
        checkThat(address.components.size >= expectedComponents.size,
            "address.components.size >= expectedComponents.size")

        suffix(address.components, expectedComponents.size)
            .zip(expectedComponents)
            .forEach {
                val (kind, names) = it.second
                checkThat(it.first.kinds[0], Is(kind), "kind")
                checkThat(it.first.name, ContainsAnyOf(*names), "name")
            }
    }

    private fun checkResponseOnSortByRoute(route: Polyline, response: Response) {
        checkSecondaryResponse(response)
        val metadata = response.metadata
        checkThat(metadata.sort?.type, Is(SortType.DISTANCE), "response sort type")
        val relevantObjectsDistances = route.distances(savedPoints!!)
        val nearestObjectsDistances = route.distances(response.points)
        checkThat(nearestObjectsDistances.average(),
                LessThan(relevantObjectsDistances.average() / 2.0),
                "sort along the route should affect average distance more")
    }
}
