package ru.yandex.yandexbus.inhouse.service.taxi.yandex

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.yandex.mapkit.LocalizedValue
import com.yandex.mapkit.Money
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.transport.taxi.RideInfo
import com.yandex.mapkit.transport.taxi.RideInfoSession
import com.yandex.mapkit.transport.taxi.RideOption
import com.yandex.mapkit.transport.taxi.TaxiManager
import com.yandex.runtime.Error
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import ru.yandex.yandexbus.inhouse.BaseTest
import ru.yandex.yandexbus.inhouse.capture
import ru.yandex.yandexbus.inhouse.service.taxi.Cost
import ru.yandex.yandexbus.inhouse.service.taxi.Ride
import ru.yandex.yandexbus.inhouse.service.taxi.TaxiManager.RequestSource
import ru.yandex.yandexbus.inhouse.service.taxi.TaxiOperator
import ru.yandex.yandexbus.inhouse.utils.exception.YandexRuntimeException
import rx.Scheduler
import rx.schedulers.TestScheduler
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger

class YandexTaxiTest : BaseTest() {

    @Test
    fun `returns correct value when mapkit request succeeds`() {
        val testRideInfo = TestRide()

        val rideOption = testRideInfo.toRideOption()
        val callsCounter = AtomicInteger()

        val taxiManager = SimpleTaxiManager(TestRideInfoImpl(rideOption), callsCounter)
        val yandexTaxi = createYandexTaxi(taxiManager)

        val receivedRide = yandexTaxi.rideInfo(from, to)
            .test()
            .assertValueCount(1)
            .onNextEvents.first()

        val expectedRide = testRideInfo.toRide(from, to)
        assertEquals(expectedRide, receivedRide)

        assertEquals(1, callsCounter.get())
    }

    @Test
    fun `fails when mapkit returns empty ride options`() {
        val callsCounter = AtomicInteger()

        val taxiManager = SimpleTaxiManager(TestRideInfoImpl(), callsCounter)
        val yandexTaxi = createYandexTaxi(taxiManager)

        yandexTaxi.rideInfo(from, to)
            .test()
            .assertError(IllegalStateException::class.java)

        assertEquals(1, callsCounter.get())
    }

    @Test
    fun `returns correct value after retry`() {
        val testRideInfo = TestRide()

        val rideOption = testRideInfo.toRideOption()
        val callsCounter = AtomicInteger()

        val taxiManager = ErrorAndThenSuccessTaxiManager(TestRideInfoImpl(rideOption), TestMapkitError, callsCounter)
        val yandexTaxi = createYandexTaxi(taxiManager)

        val ride = yandexTaxi.rideInfo(from, to)
            .test()
            .assertNoErrors()
            .assertValueCount(1)
            .onNextEvents.first()

        assertEquals(testRideInfo.toRide(from, to), ride)
        assertEquals(2, callsCounter.get())
    }

    @Test
    fun `returns error when mapkit request fails`() {
        val callsCounter = AtomicInteger()
        val testTaxiManager = FailingTaxiManager(callsCounter = callsCounter)
        val yandexTaxi = createYandexTaxi(testTaxiManager)

        yandexTaxi.rideInfo(from, to)
            .test()
            .assertNoValues()
            .assertError(YandexRuntimeException::class.java)

        assertEquals(2, callsCounter.get())
    }

    @Test
    fun `timeout after 3 seconds`() {
        val testScheduler = TestScheduler()

        val taxiManager = NoCallbacksTaxiManager()
        val yandexTaxi = createYandexTaxi(taxiManager, timeoutScheduler = testScheduler)
        val subscription = yandexTaxi.rideInfo(from, to).test()

        // First request
        testScheduler.advanceTimeBy(3L, TimeUnit.SECONDS)

        // Request should be retried
        subscription
            .assertNoErrors()
            .assertNoValues()
            .assertNotCompleted()

        // First request
        testScheduler.advanceTimeBy(3L, TimeUnit.SECONDS)

        // Should be terminated with error
        subscription.assertError(TimeoutException::class.java)
    }

    @Test
    fun `request cancelled on unsubscribe`() {
        val session = StubRideInfoSession()
        val taxiManager = TaxiManager { _, _, _ -> session }

        val yandexTaxi = createYandexTaxi(taxiManager)
        val subscription = yandexTaxi.rideInfo(from, to).test()

        assertFalse(session.isCancelled)
        subscription.unsubscribe()
        assertTrue(session.isCancelled)
    }

    @Test
    fun `requests taxi with correct parameters`() {
        testLaunchUriParameters(from, to, RequestSource.ROUTE)
        testLaunchUriParameters(from, null, RequestSource.ROUTE)
    }

    private fun testLaunchUriParameters(from: Point, to: Point?, requestSource: RequestSource) {
        val launcher = YandexTaxiLauncher { _, _ -> true }
        val taxiManager = Mockito.mock(TaxiManager::class.java)
        val yandexTaxi = createYandexTaxi(taxiManager, launcher)

        val context = Mockito.mock(Context::class.java)

        val ride = yandexTaxiRide(from, to)
        yandexTaxi.requestTaxi(context, ride, requestSource)

        val captor = ArgumentCaptor.forClass(Intent::class.java)
        Mockito.verify(context).startActivity(capture(captor))

        val passedIntent = captor.value
        assertEquals(Intent.ACTION_VIEW, passedIntent.action)

        val uri: Uri = requireNotNull(passedIntent.data)
        assertEquals(formatGeoPosition(from.latitude), uri.getQueryParameter("start-lat"))
        assertEquals(formatGeoPosition(from.longitude), uri.getQueryParameter("start-lon"))
        assertEquals(formatGeoPosition(to?.latitude), uri.getQueryParameter("end-lat"))
        assertEquals(formatGeoPosition(to?.longitude), uri.getQueryParameter("end-lon"))
        assertEquals(requestSource.source, uri.getQueryParameter("utm_medium"))
        assertEquals(requestSource.ref, uri.getQueryParameter("ref"))
    }
    
    companion object {
        private val from = Point(53.902496, 27.561481)
        private val to = Point(53.890991, 27.526205)
    }
}

private fun createYandexTaxi(
    mapkitTaxiManager: TaxiManager,
    launcher: YandexTaxiLauncher = YandexTaxiLauncher(),
    timeoutScheduler: Scheduler = TestScheduler()
): YandexTaxi {
    return YandexTaxi(mapkitTaxiManager, launcher, timeoutScheduler)
}

private fun formatGeoPosition(param: Double?): String?{
    return if (param == null) {
        null
    } else {
        String.format(Locale.ENGLISH, "%f", param)
    }
}

private fun yandexTaxiRide(pickup: Point, dropOff: Point? = null, waitingTimeEstimate: Long? = null, costEstimate: Cost? = null): Ride {
    return Ride(pickup, dropOff, waitingTimeEstimate, costEstimate, TaxiOperator.YA_TAXI)
}

private class StubRideInfoSession : RideInfoSession {

    var isCancelled: Boolean = false
        private set

    override fun retry(rideInfoListener: RideInfoSession.RideInfoListener) {
        // Do nothing
    }

    override fun cancel() {
        isCancelled = true
    }
}

// We use own implementations of TaxiManager because mockito fails to process test case when mock method should be called consequently
private class ErrorAndThenSuccessTaxiManager(
    private val rideInfo: RideInfo,
    private val error: Error,
    private val callsCounter: AtomicInteger
) : TaxiManager {
    override fun requestRideInfo(
        startPoint: Point,
        endPoint: Point,
        listener: RideInfoSession.RideInfoListener
    ): RideInfoSession {
        val call = callsCounter.incrementAndGet()
        when (call) {
            1 -> listener.onRideInfoError(error)
            2 -> listener.onRideInfoReceived(rideInfo)
            else -> AssertionError("Shouldn't be called again")
        }
        return StubRideInfoSession()
    }
}

private class FailingTaxiManager(
    private val error: Error = TestMapkitError,
    private val callsCounter: AtomicInteger
) : TaxiManager {
    override fun requestRideInfo(
        startPoint: Point,
        endPoint: Point,
        rideInfoListener: RideInfoSession.RideInfoListener
    ): RideInfoSession {
        callsCounter.incrementAndGet()
        rideInfoListener.onRideInfoError(error)
        return StubRideInfoSession()
    }
}

private class SimpleTaxiManager(
    private val rideInfo: RideInfo,
    private val callsCounter: AtomicInteger
) : TaxiManager {
    override fun requestRideInfo(
        startPoint: Point,
        endPoint: Point,
        rideInfoListener: RideInfoSession.RideInfoListener
    ): RideInfoSession {
        callsCounter.incrementAndGet()
        rideInfoListener.onRideInfoReceived(rideInfo)
        return StubRideInfoSession()
    }
}

private class NoCallbacksTaxiManager : TaxiManager {
    override fun requestRideInfo(
        startPoint: Point,
        endPoint: Point,
        rideInfoListener: RideInfoSession.RideInfoListener
    ): RideInfoSession {
        return StubRideInfoSession()
    }
}

private object TestMapkitError : Error {
    override fun isValid() = true
}

private class TestRideInfoImpl(private val rides: MutableList<RideOption> = mutableListOf()) : RideInfo() {
    constructor(rideOption: RideOption) : this(mutableListOf(rideOption))

    override fun getRideOptions() = rides
}

private class TestRide(
    private val etaMinutes: Long = 3,
    private val cost: Double = 99.0,
    private val currencyCode: String = "RUB",
    private val taxi: TaxiOperator = TaxiOperator.YA_TAXI
) {

    private val costText = "$cost ${currencyCode.toLowerCase()}"

    fun toRide(from: Point, to: Point?): Ride {
        val cost = Cost(cost, currencyCode, costText)
        return Ride(from, to, etaMinutes, cost, taxi)
    }

    fun toRideOption(): RideOption {
        val waitingTime = LocalizedValue(TimeUnit.MINUTES.toSeconds(etaMinutes).toDouble(), "$etaMinutes min")
        val money = Money(cost, costText, currencyCode)
        return RideOption(waitingTime, money, false)
    }
}
