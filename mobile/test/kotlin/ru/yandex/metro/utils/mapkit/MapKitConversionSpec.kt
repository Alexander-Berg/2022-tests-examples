package ru.yandex.metro.utils.mapkit

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.yandex.mapkit.LocalizedValue
import com.yandex.mapkit.Money
import org.amshove.kluent.shouldBeNull
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import com.yandex.mapkit.transport.taxi.RideInfo as MapKitRideInfo
import com.yandex.mapkit.transport.taxi.RideOption as MapKitRideOption

class MapKitConversionSpec : Spek({
    describe("convert MapKitRideInfo)") {
        context("MapKitRideInfo doesn't contain waiting time") {
            val mapKitRideInfo = mock<MapKitRideInfo> {
                on { rideOptions } doReturn emptyList<MapKitRideOption>()
            }

            it("should return RideInfo without waiting time") {
                val rideInfo = MapKitConversion.convert(mapKitRideInfo)
                rideInfo?.waitingTime.shouldBeNull()
            }
        }

        context("MapKitRideInfo contains negative waiting time") {
            val negativeWaitingTime = mock<LocalizedValue> {
                on { value } doReturn -1.0
                on { text } doReturn ""
            }

            val rideOption = mock<MapKitRideOption> {
                on { waitingTime } doReturn negativeWaitingTime
                on { cost } doReturn Money(100.0, "RUB", "100 rub")
            }

            val mapKitRideInfo = mock<MapKitRideInfo> {
                on { rideOptions } doReturn listOf(rideOption)
            }

            it("should return RideInfo without waiting time") {
                val rideInfo = MapKitConversion.convert(mapKitRideInfo)
                rideInfo?.waitingTime.shouldBeNull()
            }
        }

    }

})
