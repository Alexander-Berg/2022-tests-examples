package ru.yandex.metro.station.domain

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.yandex.mapkit.LocalizedValue
import com.yandex.mapkit.Money
import com.yandex.mapkit.transport.taxi.RideInfo
import com.yandex.mapkit.transport.taxi.RideInfoSession
import com.yandex.mapkit.transport.taxi.RideOption
import com.yandex.runtime.Error
import io.reactivex.schedulers.Schedulers
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldNotBeNull
import org.joda.time.Duration
import org.mockito.ArgumentCaptor
import ru.yandex.metro.ClassSpek
import ru.yandex.metro.common.domain.model.GeoPointByData
import ru.yandex.metro.taxi.domain.TaxiError
import ru.yandex.metro.taxi.domain.TaxiManagerAdapter
import java.util.concurrent.TimeUnit
import com.yandex.mapkit.transport.taxi.TaxiManager as MapKitTaxiManager

private val GEO_POINT = GeoPointByData(55.736082, 37.595073)

class TaxiManagerAdapterSpec : ClassSpek(TaxiManagerAdapter::class.java, {
    context("taxi manager has a ride info") {

        val durationMinutes = 10L
        val durationText = "10 min."

        val nativeTaxiManager by memoized {
            mock<MapKitTaxiManager> {
                val rideOption = mock<RideOption> {
                    on { waitingTime } doReturn LocalizedValue(
                            TimeUnit.MINUTES.toSeconds(durationMinutes).toDouble(),
                            durationText
                    )
                    on { cost } doReturn Money(100.0, "RUB", "100 rub")
                }
                val rideInfo = mock<RideInfo> {
                    on { rideOptions } doReturn listOf(rideOption)
                }
                val listenerCaptor = ArgumentCaptor.forClass(RideInfoSession.RideInfoListener::class.java)
                on { requestRideInfo(any(), any(), listenerCaptor.capture()) } doAnswer {
                    listenerCaptor.value.onRideInfoReceived(rideInfo)
                    mock()
                }
            }
        }
        val taxiManagerAdapter = TaxiManagerAdapter(nativeTaxiManager, Schedulers.trampoline())
        context("requesting ride info") {
            val request = taxiManagerAdapter.requestRideInfo(GEO_POINT).test()

            it("should succeed") {
                request.assertComplete()
                request.assertNoErrors()
                request.assertValueCount(1)
            }

            it("should return filled ride info") {
                val ride = request.values()[0]
                val waitingTime = ride.waitingTime.shouldNotBeNull()
                waitingTime.text shouldBeEqualTo durationText
                waitingTime.value shouldEqual Duration.standardMinutes(durationMinutes)
            }
        }
    }

    context("taxi manager returns error") {
        val nativeTaxiManager by memoized {
            mock<MapKitTaxiManager> {
                val listenerCaptor = ArgumentCaptor.forClass(RideInfoSession.RideInfoListener::class.java)
                on { requestRideInfo(any(), any(), listenerCaptor.capture()) } doAnswer {
                    val error = mock<Error>()
                    listenerCaptor.value.onRideInfoError(error)
                    mock()
                }
            }
        }
        val taxiManager = TaxiManagerAdapter(nativeTaxiManager, Schedulers.trampoline())

        context("requesting ride info") {
            it("should terminate with error") {
                val request = taxiManager.requestRideInfo(GEO_POINT).test()
                request.assertError { err -> err is TaxiError }
                request.assertNotComplete()
            }
        }
    }
})
