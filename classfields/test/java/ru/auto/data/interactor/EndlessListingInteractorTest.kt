package ru.auto.data.interactor

import io.qameta.allure.kotlin.junit4.AllureRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import ru.auto.data.model.AdditionalRequests
import ru.auto.data.model.BasicRegion
import ru.auto.data.model.feed.OffersSearchRequest
import ru.auto.data.model.filter.CarParams
import ru.auto.data.model.filter.CarSearch
import ru.auto.data.model.filter.CommonVehicleParams
import ru.auto.data.model.filter.SearchRequestByParams
import ru.auto.data.model.filter.SearchSort
import ru.auto.data.model.search.OfferLocatorCounterPromo
import ru.auto.data.model.search.OfferLocatorCounterPromoItems
import ru.auto.data.model.search.SearchContext
import rx.Single

@RunWith(AllureRunner::class)
class EndlessListingInteractorTest {

    private val offerLocatorCounterInteractor: OfferLocatorCounterInteractor = mock()
    private val interactor: EndlessListingInteractor  = EndlessListingInteractor(offerLocatorCounterInteractor)

    @Test
    fun `should make a request with a exclude rid and exclude radius if the radius is 0`() {
        val geoRadius = 0
        val request = createRequest(geoRadius = geoRadius)

        whenever(offerLocatorCounterInteractor.fetchOfferLocatorCounter(any(), any(), anyBoolean()))
            .thenReturn(Single.just(null))

        interactor.getAdditionalRequests(request).test()
            .assertCompleted()
            .assertValue(AdditionalRequests(itemCount = 0, requests = null))

        verify(offerLocatorCounterInteractor).fetchOfferLocatorCounter(
            params = createRequest(
                geoRadius = geoRadius,
                geoRid = GEO_RID,
                infinityListingSupport = true,
            ).searchRequestByParams
        )
    }

    @Test
    fun `should make a request with only exclude radius if the radius more 0`() {
        val geoRadius = 100
        val request = createRequest(geoRadius = geoRadius)

        whenever(offerLocatorCounterInteractor.fetchOfferLocatorCounter(any(), any(), anyBoolean()))
            .thenReturn(Single.just(null))

        interactor.getAdditionalRequests(request).test()
            .assertCompleted()
            .assertValue(AdditionalRequests(itemCount = 0, requests = null))

        verify(offerLocatorCounterInteractor).fetchOfferLocatorCounter(
            params = createRequest(
                geoRadius = geoRadius,
                geoRid = GEO_RID,
                infinityListingSupport = true,
            ).searchRequestByParams
        )
    }

    @Test
    fun `should return requests without collapsing and first additional request with exclude when radius is 0`() {
        val geoRadius = 0

        val items = listOf(
            createExpandItem(20, 100),
            createExpandItem(40, 200),
            createExpandItem(60, 300)
        ).toItems()

        whenever(offerLocatorCounterInteractor.fetchOfferLocatorCounter(any(), any(), anyBoolean()))
            .thenReturn(Single.just(items))

        val request = createRequest(geoRadius = geoRadius)
        val expectedRequests = listOf(
            createEndlessListingRequest(geoRadius = 100, excludeGeoRid = GEO_RID),
            createEndlessListingRequest(geoRadius = 200, excludeGeoRadius = 100),
            createEndlessListingRequest(geoRadius = 300, excludeGeoRadius = 200)
        ).toAdditionalRequests(count = 60)

        interactor.getAdditionalRequests(request).test()
            .assertCompleted()
            .assertValue(expectedRequests)
    }

    @Test
    fun `should return requests without collapsing and first additional request without exclude when radius more 0`() {
        val geoRadius = 100

        val items = listOf(
            createExpandItem(20, 200),
            createExpandItem(40, 300),
            createExpandItem(60, 400)
        ).toItems()

        whenever(offerLocatorCounterInteractor.fetchOfferLocatorCounter(any(), any(), anyBoolean()))
            .thenReturn(Single.just(items))

        val request = createRequest(geoRadius = geoRadius)
        val expectedRequests = listOf(
            createEndlessListingRequest(geoRadius = 200, excludeGeoRadius = 100),
            createEndlessListingRequest(geoRadius = 300, excludeGeoRadius = 200),
            createEndlessListingRequest(geoRadius = 400, excludeGeoRadius = 300)
        ).toAdditionalRequests(count = 60)

        interactor.getAdditionalRequests(request).test()
            .assertCompleted()
            .assertValue(expectedRequests)
    }

    @Test
    fun `should return requests with collapsing if items are empty`() {
        val geoRadius = 100

        val items = listOf(
            createExpandItem(0, 200),
            createExpandItem(20, 300),
            createExpandItem(60, 400)
        ).toItems()

        whenever(offerLocatorCounterInteractor.fetchOfferLocatorCounter(any(), any(), anyBoolean()))
            .thenReturn(Single.just(items))

        val request = createRequest(geoRadius = geoRadius)
        val expectedRequests = listOf(
            createEndlessListingRequest(geoRadius = 300, excludeGeoRadius = 100),
            createEndlessListingRequest(geoRadius = 400, excludeGeoRadius = 300)
        ).toAdditionalRequests(count = 60)

        interactor.getAdditionalRequests(request).test()
            .assertCompleted()
            .assertValue(expectedRequests)
    }

    @Test
    fun `should return requests with collapsing if items in the ring have not changed`() {
        val geoRadius = 100

        val items = listOf(
            createExpandItem(20, 200),
            createExpandItem(20, 300),
            createExpandItem(60, 400)
        ).toItems()

        whenever(offerLocatorCounterInteractor.fetchOfferLocatorCounter(any(), any(), anyBoolean()))
            .thenReturn(Single.just(items))

        val request = createRequest(geoRadius = geoRadius)
        val expectedRequests = listOf(
            createEndlessListingRequest(geoRadius = 300, excludeGeoRadius = 100),
            createEndlessListingRequest(geoRadius = 400, excludeGeoRadius = 300)
        ).toAdditionalRequests(count = 60)

        interactor.getAdditionalRequests(request).test()
            .assertCompleted()
            .assertValue(expectedRequests)
    }

    @Test
    fun `should return requests with collapsing if items are smaller than page size (at the beginning)`() {
        val geoRadius = 100

        val items = listOf(
            createExpandItem(3, 200),
            createExpandItem(18, 300),
            createExpandItem(24, 400)
        ).toItems()

        whenever(offerLocatorCounterInteractor.fetchOfferLocatorCounter(any(), any(), anyBoolean()))
            .thenReturn(Single.just(items))

        val request = createRequest(geoRadius = geoRadius)
        val expectedRequests = listOf(
            createEndlessListingRequest(geoRadius = 300, excludeGeoRadius = 100),
            createEndlessListingRequest(geoRadius = 400, excludeGeoRadius = 300)
        ).toAdditionalRequests(count = 24)

        interactor.getAdditionalRequests(request).test()
            .assertCompleted()
            .assertValue(expectedRequests)
    }

    @Test
    fun `should return requests with collapsing if items are smaller than page size (in the middle)`() {
        val geoRadius = 100

        val items = listOf(
            createExpandItem(20, 200),
            createExpandItem(25, 300),
            createExpandItem(30, 400),
            createExpandItem(60, 500)
        ).toItems()

        whenever(offerLocatorCounterInteractor.fetchOfferLocatorCounter(any(), any(), anyBoolean()))
            .thenReturn(Single.just(items))

        val request = createRequest(geoRadius = geoRadius)
        val expectedRequests = listOf(
            createEndlessListingRequest(geoRadius = 200, excludeGeoRadius = 100),
            createEndlessListingRequest(geoRadius = 400, excludeGeoRadius = 200),
            createEndlessListingRequest(geoRadius = 500, excludeGeoRadius = 400)
        ).toAdditionalRequests(count = 60)

        interactor.getAdditionalRequests(request).test()
            .assertCompleted()
            .assertValue(expectedRequests)
    }

    @Test
    fun `should return requests with collapsing if items are smaller than page size (at the end)`() {
        val geoRadius = 100

        val items = listOf(
            createExpandItem(20, 200),
            createExpandItem(40, 300),
            createExpandItem(42, 400),
            createExpandItem(60, 500)
        ).toItems()

        whenever(offerLocatorCounterInteractor.fetchOfferLocatorCounter(any(), any(), anyBoolean()))
            .thenReturn(Single.just(items))

        val request = createRequest(geoRadius = geoRadius)
        val expectedRequests = listOf(
            createEndlessListingRequest(geoRadius = 200, excludeGeoRadius = 100),
            createEndlessListingRequest(geoRadius = 300, excludeGeoRadius = 200),
            createEndlessListingRequest(geoRadius = 500, excludeGeoRadius = 300)
        ).toAdditionalRequests(count = 60)

        interactor.getAdditionalRequests(request).test()
            .assertCompleted()
            .assertValue(expectedRequests)
    }

    @Test
    fun `should return request with radius is null and exclude_radius is 1000 when get a radius 1100 (ALL_RUSSIA_RADIUS)`() {
        val geoRadius = 100

        val items = listOf(
            createExpandItem(20, 200),
            createExpandItem(80, 1000),
            createExpandItem(100, 1100),
        ).toItems()

        whenever(offerLocatorCounterInteractor.fetchOfferLocatorCounter(any(), any(), anyBoolean()))
            .thenReturn(Single.just(items))

        val request = createRequest(geoRadius = geoRadius)
        val expectedRequests = listOf(
            createEndlessListingRequest(geoRadius = 200, excludeGeoRadius = 100),
            createEndlessListingRequest(geoRadius = 1000, excludeGeoRadius = 200),
            createEndlessListingRequest(geoRadius = null, excludeGeoRadius = 1000)
        ).toAdditionalRequests(count = 100)

        interactor.getAdditionalRequests(request).test()
            .assertCompleted()
            .assertValue(expectedRequests)
    }

    private fun createEndlessListingRequest(
        geoRadius: Int?,
        geoRid: String = GEO_RID,
        excludeGeoRadius: Int? = null,
        excludeGeoRid: String? = null,
    ) = createRequest(
        geoRadius = geoRadius,
        geoRid = geoRid,
        excludeGeoRadius = excludeGeoRadius,
        excludeGeoRid = excludeGeoRid,
        isAdditionalRequest = true,
        infinityListingSupport = true
    )

    private fun createRequest(
        geoRadius: Int?,
        geoRid: String = GEO_RID,
        excludeGeoRadius: Int? = null,
        excludeGeoRid: String? = null,
        isAdditionalRequest: Boolean = false,
        infinityListingSupport: Boolean = false,
    ): OffersSearchRequest = OffersSearchRequest(
        savedSearchId = null,
        searchRequestByParams = SearchRequestByParams(
            search = CarSearch(
                carParams = CarParams(),
                commonParams = CommonVehicleParams(
                    geoRadius = geoRadius,
                    regions = listOf(BasicRegion(id = geoRid, name = geoRid)),
                    excludeGeoRadius = excludeGeoRadius,
                    excludeRid = excludeGeoRid,
                    isAdditionalRequest = isAdditionalRequest,
                    infinityListingSupport = infinityListingSupport
                )
            ),
            context = SearchContext.DEFAULT,
            sort = SearchSort.EQUIPMENT_COUNT_ASC
        )
    )

    private fun List<OffersSearchRequest>.toAdditionalRequests(count: Int) = AdditionalRequests(count, this)

    private fun createExpandItem(
        offersCount: Int,
        radius: Int
    ) = OfferLocatorCounterPromo(
        offersCount = offersCount,
        distance = radius,
        offers = emptyList()
    )

    private fun List<OfferLocatorCounterPromo>.toItems() =
        OfferLocatorCounterPromoItems(
            region = BasicRegion(id = "0", name = "test"),
            items = this
        )

    companion object {
        private const val GEO_RID = "geo_rid"
    }
}
