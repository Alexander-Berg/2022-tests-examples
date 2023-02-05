package ru.yandex.yandexbus.inhouse.service.taxi.uber

import com.uber.sdk.android.rides.RideParameters
import com.uber.sdk.android.rides.RideRequestBehavior
import com.uber.sdk.rides.client.model.PriceEstimatesResponse
import com.uber.sdk.rides.client.model.TimeEstimatesResponse
import com.uber.sdk.rides.client.services.RidesService
import com.yandex.mapkit.geometry.Point
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import retrofit2.Call
import ru.yandex.yandexbus.inhouse.BaseTest
import ru.yandex.yandexbus.inhouse.capture
import ru.yandex.yandexbus.inhouse.eq
import ru.yandex.yandexbus.inhouse.service.taxi.Cost
import ru.yandex.yandexbus.inhouse.service.taxi.Ride
import ru.yandex.yandexbus.inhouse.service.taxi.TaxiManager
import ru.yandex.yandexbus.inhouse.service.taxi.TaxiOperator
import ru.yandex.yandexbus.inhouse.utils.MathU
import ru.yandex.yandexbus.inhouse.whenever
import java.util.concurrent.TimeUnit

class UberTaxiTest : BaseTest() {

    @Mock
    lateinit var rideRequestBehavior: RideRequestBehavior

    @Mock
    lateinit var ridesService: RidesService

    private lateinit var uberTaxi: UberTaxi

    override fun setUp() {
        super.setUp()
        uberTaxi = UberTaxi(rideRequestBehavior, ridesService)
    }

    @Test
    fun `correct ride when both requests succeed`() {
        mockPickupTimeEstimate(from, null, timeEstimate)
        mockPriceEstimate(from, to, priceEstimate)

        val expectedRide = uberRide(from, to, timeEstimateMinutes, cost)

        uberTaxi.rideInfo(from, to)
            .test()
            .assertValue(expectedRide)
    }

    @Test
    fun `error when pickup time estimate request fails`() {
        val noPickupEstimateError = Exception("no pickup estimate")

        mockPickupTimeEstimate(from, null, noPickupEstimateError)
        mockPriceEstimate(from, to, priceEstimate)

        uberTaxi.rideInfo(from, to)
            .test()
            .assertError(noPickupEstimateError)
    }

    @Test
    fun `error when price estimate request fails`() {
        val noPriceEstimateError = Exception("no price estimate")

        mockPickupTimeEstimate(from, null, timeEstimate)
        mockPriceEstimate(from, to, noPriceEstimateError)

        uberTaxi.rideInfo(from, to)
            .test()
            .assertError(noPriceEstimateError)
    }

    @Test
    fun `error when pickup time estimate request fails with network error`() {
        mockPickupTimeEstimate(from, null, NetworkErrorCall())
        mockPriceEstimate(from, to, priceEstimate)

        uberTaxi.rideInfo(from, to)
            .test()
            .assertError(UberApiException::class.java)
    }

    @Test
    fun `error when price estimate request succeeds with network error`() {
        mockPickupTimeEstimate(from, null, timeEstimate)
        mockPriceEstimate(from, to, NetworkErrorCall())

        uberTaxi.rideInfo(from, to)
            .test()
            .assertError(UberApiException::class.java)
    }

    @Test
    fun `no time estimate when sdk sends empty list`() {
        mockPickupTimeEstimate(from, null, TestTimeEstimatesResponse(emptyList()))
        mockPriceEstimate(from, to, priceEstimate)

        val expectedRide = uberRide(from, to, null, cost)

        uberTaxi.rideInfo(from, to)
            .test()
            .assertValue(expectedRide)
    }

    @Test
    fun `no price estimate when sdk sends empty list`() {
        mockPickupTimeEstimate(from, null, timeEstimate)
        mockPriceEstimate(from, to, TestPriceEstimatesResponse(emptyList()))

        val expectedRide = uberRide(from, to, timeEstimateMinutes, null)

        uberTaxi.rideInfo(from, to)
            .test()
            .assertValue(expectedRide)
    }

    @Test
    fun `no price estimate when destination is unknown`() {
        mockPickupTimeEstimate(from, null, timeEstimate)

        val expectedRide = uberRide(from, null, timeEstimateMinutes, null)

        uberTaxi.rideInfo(from, null)
            .test()
            .assertValue(expectedRide)

        // getPriceEstimates should not be called when destination is unknown
        verify(ridesService).getPickupTimeEstimate(from.latitude.toFloat(), from.longitude.toFloat(), null)
        verifyNoMoreInteractions(ridesService)
    }

    @Test
    fun `cancels calls on unsubscribe`() {
        val timeEstimateCall: Call<TimeEstimatesResponse> = EmptyCall()
        val priceEstimateCall: Call<PriceEstimatesResponse> = EmptyCall()

        mockPickupTimeEstimate(from, null, timeEstimateCall)
        mockPriceEstimate(from, to, priceEstimateCall)

        val subscription = uberTaxi.rideInfo(from, to).test()

        assertFalse(timeEstimateCall.isCanceled)
        assertFalse(priceEstimateCall.isCanceled)

        subscription.unsubscribe()

        assertTrue(timeEstimateCall.isCanceled)
        assertTrue(priceEstimateCall.isCanceled)
    }

    @Test
    fun `requests taxi with correct parameters`() {
        val ride = uberRide(from, to)

        uberTaxi.requestTaxi(context, ride, TaxiManager.RequestSource.ROUTE)

        val captor = ArgumentCaptor.forClass(RideParameters::class.java)
        verify(rideRequestBehavior).requestRide(eq(context), capture(captor))
        assertRequestParamsEquals(from, to, captor.value)
    }

    @Test
    fun `requests taxi with correct parameters when destination point is unknown`() {
        val ride = uberRide(from)

        uberTaxi.requestTaxi(context, ride, TaxiManager.RequestSource.ROUTE)

        val captor = ArgumentCaptor.forClass(RideParameters::class.java)
        verify(rideRequestBehavior).requestRide(eq(context), capture(captor))

        assertRequestParamsEquals(from, null, captor.value)
    }

    private fun assertRequestParamsEquals(from: Point, to: Point?, params: RideParameters) {
        assertEquals(from.latitude, params.pickupLatitude)
        assertEquals(from.longitude, params.pickupLongitude)
        assertEquals(to?.latitude, params.dropoffLatitude)
        assertEquals(to?.longitude, params.dropoffLongitude)
    }

    private fun mockPickupTimeEstimate(from: Point, productId: String?, call: Call<TimeEstimatesResponse>) {
        whenever(ridesService.getPickupTimeEstimate(from.latitude.toFloat(), from.longitude.toFloat(), productId))
            .thenReturn(call)
    }

    private fun mockPickupTimeEstimate(from: Point, productId: String?, error: Throwable) {
        mockPickupTimeEstimate(from, productId, ErrorCall(error))
    }

    private fun mockPickupTimeEstimate(from: Point, productId: String?, response: TimeEstimatesResponse) {
        mockPickupTimeEstimate(from, productId, SuccessfulCall(response))
    }

    private fun mockPriceEstimate(from: Point, to: Point, call: Call<PriceEstimatesResponse>) {
        whenever(ridesService.getPriceEstimates(from.latitude.toFloat(), from.longitude.toFloat(), to.latitude.toFloat(), to.longitude.toFloat()))
            .thenReturn(call)
    }

    private fun mockPriceEstimate(from: Point, to: Point, error: Throwable) {
        mockPriceEstimate(from, to, ErrorCall(error))
    }

    private fun mockPriceEstimate(from: Point, to: Point, priceEstimate: PriceEstimatesResponse) {
        mockPriceEstimate(from, to, SuccessfulCall(priceEstimate))
    }

    companion object {
        private val from = Point(53.902496, 27.561481)
        private val to = Point(53.890991, 27.526205)


        private const val timeEstimateMinutes = 5L
        private val timeEstimateSeconds = TimeUnit.MINUTES.toSeconds(timeEstimateMinutes).toInt()
        private val timeEstimate = TestTimeEstimatesResponse(TestTimeEstimate(timeEstimateSeconds))


        private const val priceEstimateLow = 3
        private const val priceEstimateHigh = 5
        private val priceEstimateAvg = MathU.average(priceEstimateLow, priceEstimateHigh)
        private const val currencyCode = "BYN"
        private val estimateText = "$priceEstimateAvg $currencyCode"

        private val cost = Cost(priceEstimateAvg, currencyCode, estimateText)
        private val priceEstimate = TestPriceEstimatesResponse(TestPriceEstimate(priceEstimateLow, priceEstimateHigh, estimateText, currencyCode))
    }
}

private fun uberRide(pickup: Point, dropOff: Point? = null, waitingTimeEstimate: Long? = null, costEstimate: Cost? = null): Ride {
    return Ride(pickup, dropOff, waitingTimeEstimate, costEstimate, TaxiOperator.UBER)
}