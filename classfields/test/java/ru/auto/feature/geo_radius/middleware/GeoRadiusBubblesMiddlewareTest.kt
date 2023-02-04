package ru.auto.feature.geo_radius.middleware

import io.qameta.allure.kotlin.junit4.AllureRunner
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.auto.ara.data.entities.form.Option
import ru.auto.ara.util.android.AndroidMultiOptionsProvider
import ru.auto.data.interactor.OfferLocatorCounterInteractor
import ru.auto.data.model.BasicRegion
import ru.auto.data.model.common.Page
import ru.auto.data.model.feed.OffersSearchRequest
import ru.auto.data.model.feed.model.GeoRadiusBubblesModel
import ru.auto.data.model.filter.CarParams
import ru.auto.data.model.filter.CarSearch
import ru.auto.data.model.filter.CommonVehicleParams
import ru.auto.data.model.filter.SearchRequestByParams
import ru.auto.data.model.filter.SearchSort
import ru.auto.data.model.search.OfferLocatorCounterPromo
import ru.auto.data.model.search.OfferLocatorCounterPromoItems
import ru.auto.data.model.search.SearchContext
import ru.auto.data.repository.feed.loader.AdditionalInfo
import ru.auto.data.repository.feed.loader.post.FeedRequest
import ru.auto.data.repository.feed.loader.post.IFeedState
import rx.Single


@RunWith(AllureRunner::class) class GeoRadiusBubblesMiddlewareTest {

    private val offerLocatorCounterInteractor: OfferLocatorCounterInteractor = mock()
    private val options: AndroidMultiOptionsProvider = mock()
    private val middleware = GeoRadiusBubblesMiddleware(offerLocatorCounterInteractor, options)

    @Test
    fun `should return all geo radius bubbles`() {
        val currentRadius = 0
        val radiusList = listOf((5 to 0), (10 to 100), (15 to 200), (20 to 300), (25 to 400), (30 to 500), (35 to 1000))
        val feedState: IFeedState<OffersSearchRequest> = mock {
            on { getPage() }.thenReturn(Page(0, 1))
            on { getActualRequest() }.thenReturn(createRequest(radius = currentRadius))
        }

        mockOfferLocatorCounterRequest(radiusList)
        whenever(options.get(any())).thenReturn(createOptions(listOf(0, 100, 200, 300, 400, 500, 1000)))

        middleware.load(feedState, mock()).test()
            .assertCompleted()
            .assertValue(listOf(createAdditional(
                currentRadius = currentRadius,
                radiusList = radiusList
            )))
    }

    @Test
    fun `should return geo radius bubbles with 0 radius when 0 should be skip`() {
        val currentRadius = 100
        val radiusList = listOf((5 to 0), (5 to 100), (15 to 200), (20 to 300), (25 to 400), (30 to 500), (35 to 1000))
        val expectedRadiusList = listOf((5 to 0), (5 to 100), (15 to 200), (20 to 300), (25 to 400), (30 to 500), (35 to 1000))
        val feedState: IFeedState<OffersSearchRequest> = mock {
            on { getPage() }.thenReturn(Page(0, 1))
            on { getActualRequest() }.thenReturn(createRequest(radius = currentRadius))
        }

        mockOfferLocatorCounterRequest(radiusList)
        whenever(options.get(any())).thenReturn(createOptions(listOf(0, 100, 200, 300, 400, 500, 1000)))

        middleware.load(feedState, mock()).test()
            .assertCompleted()
            .assertValue(listOf(createAdditional(
                currentRadius = currentRadius,
                radiusList = expectedRadiusList
            )))
    }

    @Test
    fun `should return geo radius bubbles with filter by options`() {
        val currentRadius = 0
        val radiusList = listOf((5 to 0), (10 to 100), (15 to 200), (20 to 300), (25 to 400), (30 to 500), (35 to 1000))
        val expectedRadiusList = listOf((5 to 0), (10 to 100), (25 to 400), (30 to 500), (35 to 1000))
        val feedState: IFeedState<OffersSearchRequest> = mock {
            on { getPage() }.thenReturn(Page(0, 1))
            on { getActualRequest() }.thenReturn(createRequest(radius = currentRadius))
        }

        mockOfferLocatorCounterRequest(radiusList)
        whenever(options.get(any())).thenReturn(createOptions(listOf(0, 100, 400, 500, 1000)))

        middleware.load(feedState, mock()).test()
            .assertCompleted()
            .assertValue(listOf(createAdditional(
                currentRadius = currentRadius,
                radiusList = expectedRadiusList
            )))
    }

    @Test
    fun `should return geo radius bubbles with current radius when current radius was filtered`() {
        val currentRadius = 200
        val radiusList = listOf((5 to 0), (10 to 100), (10 to 200), (20 to 300), (25 to 400), (30 to 500), (30 to 1000))
        val expectedRadiusList = listOf((5 to 0), (10 to 200), (20 to 300), (25 to 400), (30 to 500))
        val feedState: IFeedState<OffersSearchRequest> = mock {
            on { getPage() }.thenReturn(Page(0, 1))
            on { getActualRequest() }.thenReturn(createRequest(radius = currentRadius))
        }

        mockOfferLocatorCounterRequest(radiusList)
        whenever(options.get(any())).thenReturn(createOptions(listOf(0, 100, 200, 300, 400, 500, 1000)))

        middleware.load(feedState, mock()).test()
            .assertCompleted()
            .assertValue(listOf(createAdditional(
                currentRadius = currentRadius,
                radiusList = expectedRadiusList
            )))
    }

    @Test
    fun `should return geo radius bubbles with current radius and filter previous radius of the same length`() {
        val currentRadius = 300
        val radiusList = listOf((10 to 0), (10 to 100), (10 to 200), (10 to 300), (25 to 400), (30 to 500), (30 to 1000))
        val expectedRadiusList = listOf((10 to 0), (10 to 300), (25 to 400), (30 to 500))
        val feedState: IFeedState<OffersSearchRequest> = mock {
            on { getPage() }.thenReturn(Page(0, 1))
            on { getActualRequest() }.thenReturn(createRequest(radius = currentRadius))
        }

        mockOfferLocatorCounterRequest(radiusList)
        whenever(options.get(any())).thenReturn(createOptions(listOf(0, 100, 200, 300, 400, 500, 1000)))

        middleware.load(feedState, mock()).test()
            .assertCompleted()
            .assertValue(listOf(createAdditional(
                currentRadius = currentRadius,
                radiusList = expectedRadiusList
            )))
    }

    @Test
    fun `should return geo radius bubbles with filter by count offers`() {
        val currentRadius = 0
        val radiusList = listOf((5 to 0), (10 to 100), (10 to 200), (20 to 300), (25 to 400), (30 to 500), (30 to 1000))
        val expectedRadiusList = listOf((5 to 0), (10 to 100), (20 to 300), (25 to 400), (30 to 500))
        val feedState: IFeedState<OffersSearchRequest> = mock {
            on { getPage() }.thenReturn(Page(0, 1))
            on { getActualRequest() }.thenReturn(createRequest(radius = currentRadius))
        }

        mockOfferLocatorCounterRequest(radiusList)
        whenever(options.get(any())).thenReturn(createOptions(listOf(0, 100, 200, 300, 400, 500, 1000)))

        middleware.load(feedState, mock()).test()
            .assertCompleted()
            .assertValue(listOf(createAdditional(
                currentRadius = currentRadius,
                radiusList = expectedRadiusList
            )))
    }

    @Test
    fun `should return geo radius bubbles with filter zero offers`() {
        val currentRadius = 200
        val radiusList = listOf((0 to 0), (10 to 100), (15 to 200), (20 to 300), (25 to 400), (30 to 500), (35 to 1000))
        val expectedRadiusList = listOf((0 to 0), (10 to 100), (15 to 200), (20 to 300), (25 to 400), (30 to 500), (35 to 1000))
        val feedState: IFeedState<OffersSearchRequest> = mock {
            on { getPage() }.thenReturn(Page(0, 1))
            on { getActualRequest() }.thenReturn(createRequest(radius = currentRadius))
        }

        mockOfferLocatorCounterRequest(radiusList)
        whenever(options.get(any())).thenReturn(createOptions(listOf(0, 100, 200, 300, 400, 500, 1000)))

        middleware.load(feedState, mock()).test()
            .assertCompleted()
            .assertValue(listOf(createAdditional(
                currentRadius = currentRadius,
                radiusList = expectedRadiusList
            )))
    }

    @Test
    fun `should return geo radius bubbles with filter by options and offers count`() {
        val currentRadius = 200
        val radiusList = listOf(
            (5 to 0), (10 to 100), (15 to 200), (20 to 300), (25 to 400), (30 to 500),
            (32 to 600), (34 to 700), (35 to 800), (35 to 900),
            (35 to 1000)
        )
        val expectedRadiusList = listOf((5 to 0), (10 to 100), (15 to 200), (20 to 300), (25 to 400), (30 to 500), (35 to 1000))
        val feedState: IFeedState<OffersSearchRequest> = mock {
            on { getPage() }.thenReturn(Page(0, 1))
            on { getActualRequest() }.thenReturn(createRequest(radius = currentRadius))
        }

        mockOfferLocatorCounterRequest(radiusList)
        whenever(options.get(any())).thenReturn(createOptions(listOf(0, 100, 200, 300, 400, 500, 1000)))

        middleware.load(feedState, mock()).test()
            .assertCompleted()
            .assertValue(listOf(createAdditional(
                currentRadius = currentRadius,
                radiusList = expectedRadiusList
            )))
    }

    private fun mockOfferLocatorCounterRequest(radiusList: List<Pair<Int, Int>>) {
        whenever(offerLocatorCounterInteractor.fetchOfferLocatorCounter(
            params = any(),
            vehicleCategories = any(),
            withoutFilter = any()
        )).thenReturn(Single.just(createItems(radiusList)))
    }

    private fun createItems(radiusList: List<Pair<Int, Int>>) = OfferLocatorCounterPromoItems(
        region = BasicRegion(id = "0", name = "Test"),
        items = radiusList.map { OfferLocatorCounterPromo(it.first, it.second, emptyList()) }
    )

    private fun createOptions(radiusList: List<Int>) = radiusList.map { Option(it.toString(), "") }

    private fun createAdditional(currentRadius: Int, radiusList: List<Pair<Int, Int>>) =
        AdditionalInfo(
            itemsToBeginning = listOf(GeoRadiusBubblesModel(
                cityName = "Test",
                currentRadius = currentRadius,
                items = radiusList.map { GeoRadiusBubblesModel.GeoRadiusItem(it.first, it.second) }
            ))
        )

    private fun createRequest(radius: Int?) = FeedRequest(
        OffersSearchRequest(
            savedSearchId = null,
            searchRequestByParams = SearchRequestByParams(
                search = CarSearch(
                    carParams = CarParams(),
                    commonParams = CommonVehicleParams(
                        geoRadius = radius,
                        geoRadiusSupport = true
                    )
                ),
                context = SearchContext.DEFAULT,
                sort = SearchSort.YEAR_ASC
            )
        ),
        page = Page(0, 1)
    )
}
