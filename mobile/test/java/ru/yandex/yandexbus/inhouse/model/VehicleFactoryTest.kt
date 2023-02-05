package ru.yandex.yandexbus.inhouse.model

import com.yandex.mapkit.Time
import com.yandex.mapkit.transport.masstransit.Line
import com.yandex.mapkit.transport.masstransit.Stop
import com.yandex.mapkit.transport.masstransit.Vehicle
import com.yandex.mapkit.transport.masstransit.VehicleStop
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import ru.yandex.yandexbus.inhouse.BaseTest
import ru.yandex.yandexbus.inhouse.extensions.mapkit.GeoPlaces
import ru.yandex.yandexbus.inhouse.whenever

private typealias MapkitVehicle = com.yandex.mapkit.transport.masstransit.Vehicle

class VehicleFactoryTest : BaseTest() {

    @Mock
    private lateinit var mapkitVehicle: MapkitVehicle

    @Mock
    private lateinit var line: Line

    @Before
    override fun setUp() {
        super.setUp()

        whenever(mapkitVehicle.id).thenReturn(VEHICLE_ID)
        whenever(mapkitVehicle.line).thenReturn(line)
        whenever(mapkitVehicle.threadId).thenReturn(THREAD_ID)

        whenever(mapkitVehicle.position).thenReturn(GeoPlaces.Minsk.CENTER)

        val props = mock(Vehicle.Properties::class.java)
        whenever(props.airConditioning).thenReturn(null)
        whenever(props.bikesAllowed).thenReturn(null)
        whenever(props.lowFloor).thenReturn(null)
        whenever(props.toDepot).thenReturn(null)
        whenever(props.wheelchairAccessible).thenReturn(null)
        whenever(mapkitVehicle.properties).thenReturn(props)

        whenever(mapkitVehicle.stops).thenReturn(listOf(VEHICLE_STOP_1, VEHICLE_STOP_2))

        whenever(line.id).thenReturn(LINE_ID)
        whenever(line.name).thenReturn(LINE_NAME)
        whenever(line.style).thenReturn(null)
    }

    @Test
    fun `vehicle type defaults to UNKNOWN if none of provided vehicle types is known`() {
        whenever(line.vehicleTypes).thenReturn(listOf("some_unknown_vehicle_type", "some_other_unknown_vehicle_type"))
        val vehicle = createVehicle(mapkitVehicle)
        assertEquals(listOf(VehicleType.UNKNOWN, VehicleType.UNKNOWN), vehicle.types)
        assertEquals(VehicleType.UNKNOWN, vehicle.supportedType)
    }

    @Test
    fun `supported vehicle type is set to first supported from the provided vehicle types`() {
        whenever(line.vehicleTypes).thenReturn(listOf("some_unknown_vehicle_type", VehicleType.RAILWAY.rawType))
        val vehicle = createVehicle(mapkitVehicle)
        assertEquals(listOf(VehicleType.UNKNOWN, VehicleType.RAILWAY), vehicle.types)
        assertEquals(VehicleType.RAILWAY, vehicle.supportedType)
    }

    private companion object {

        const val VEHICLE_ID = "vehicleId"
        const val LINE_ID = "bus_1"
        const val LINE_NAME = "1"
        const val THREAD_ID = "bus_1_A"

        val VEHICLE_STOP_1 = makeVehicleStop(makeStop(id = "stop_1_id", name = "stop_1_name"))
        val VEHICLE_STOP_2 = makeVehicleStop(makeStop(id = "stop_2_id", name = "stop_2_name"))

        fun makeStop(id: String, name: String): Stop {
            val stop = mock(Stop::class.java)
            whenever(stop.id).thenReturn(id)
            whenever(stop.name).thenReturn(name)
            return stop
        }

        fun makeVehicleStop(stop: Stop): VehicleStop {
            val vehicleStop = mock(VehicleStop::class.java)
            val estimation = mock(VehicleStop.Estimation::class.java)
            val arrivalTime = mock(Time::class.java)

            whenever(vehicleStop.stop).thenReturn(stop)
            whenever(vehicleStop.estimation).thenReturn(estimation)

            whenever(estimation.arrivalTime).thenReturn(arrivalTime)
            whenever(arrivalTime.text).thenReturn("")
            whenever(arrivalTime.tzOffset).thenReturn(0)
            whenever(arrivalTime.value).thenReturn(0L)

            return vehicleStop
        }
    }
}
