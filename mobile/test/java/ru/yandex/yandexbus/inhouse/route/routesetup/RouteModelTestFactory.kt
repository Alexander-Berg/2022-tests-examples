package ru.yandex.yandexbus.inhouse.route.routesetup

import com.yandex.mapkit.geometry.BoundingBox
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polyline
import com.yandex.mapkit.geometry.Subpolyline
import com.yandex.mapkit.transport.masstransit.Route
import org.mockito.Mockito
import ru.yandex.yandexbus.inhouse.model.VehicleType
import ru.yandex.yandexbus.inhouse.model.alert.ThreadAlert
import ru.yandex.yandexbus.inhouse.model.route.BikeRouteModel
import ru.yandex.yandexbus.inhouse.model.route.MasstransitRouteModel
import ru.yandex.yandexbus.inhouse.model.route.PedestrianRouteModel
import ru.yandex.yandexbus.inhouse.model.route.RouteModel
import ru.yandex.yandexbus.inhouse.model.route.RoutePoint
import ru.yandex.yandexbus.inhouse.model.route.TaxiRouteModel
import ru.yandex.yandexbus.inhouse.service.taxi.Ride
import ru.yandex.yandexbus.inhouse.utils.datetime.DateTime

fun testMasstransitRouteModel(
    uri: String? = null,
    boundingBox: BoundingBox = Mockito.mock(BoundingBox::class.java),
    departurePoint: RoutePoint = Mockito.mock(RoutePoint::class.java),
    destinationPoint: RoutePoint = Mockito.mock(RoutePoint::class.java),
    sections: List<RouteModel.RouteSection> = emptyList(),
    transfersCount: Int = 0,
    travelTimeSeconds: Double = 0.0,
    travelTimeText: String = "",
    arrivalEstimation: DateTime? = null,
    departureEstimation: DateTime? = null,
    acceptTypes: List<VehicleType> = emptyList(),
    avoidTypes: List<VehicleType> = emptyList(),
    originalRoute: Route? = null
) = MasstransitRouteModel(
    uri,
    boundingBox,
    departurePoint,
    destinationPoint,
    sections,
    transfersCount,
    travelTimeSeconds,
    travelTimeText,
    arrivalEstimation,
    departureEstimation,
    acceptTypes,
    avoidTypes,
    originalRoute
)

fun testPedestrianRouteModel(
    uri: String? = null,
    boundingBox: BoundingBox = Mockito.mock(BoundingBox::class.java),
    departurePoint: RoutePoint = Mockito.mock(RoutePoint::class.java),
    destinationPoint: RoutePoint = Mockito.mock(RoutePoint::class.java),
    sections: List<RouteModel.RouteSection> = emptyList(),
    transfersCount: Int = 0,
    travelTimeSeconds: Double = 0.0,
    travelTimeText: String = "",
    arrivalEstimation: DateTime? = null,
    departureEstimation: DateTime? = null,
    distanceText: String = ""
) = PedestrianRouteModel(
    uri,
    boundingBox,
    departurePoint,
    destinationPoint,
    sections,
    transfersCount,
    travelTimeSeconds,
    travelTimeText,
    arrivalEstimation,
    departureEstimation,
    distanceText
)

fun testTaxiRouteModel(
    uri: String? = null,
    boundingBox: BoundingBox = Mockito.mock(BoundingBox::class.java),
    departurePoint: RoutePoint = Mockito.mock(RoutePoint::class.java),
    destinationPoint: RoutePoint = Mockito.mock(RoutePoint::class.java),
    sections: List<RouteModel.RouteSection> = emptyList(),
    transfersCount: Int = 0,
    travelTimeSeconds: Double = 0.0,
    travelTimeText: String = "",
    arrivalEstimation: DateTime? = null,
    departureEstimation: DateTime? = null,
    ride: Ride = Mockito.mock(Ride::class.java),
    rideDistanceMeters: Double = 1000.0
) = TaxiRouteModel(
    uri,
    boundingBox,
    departurePoint,
    destinationPoint,
    sections,
    transfersCount,
    travelTimeSeconds,
    travelTimeText,
    arrivalEstimation,
    departureEstimation,
    ride,
    rideDistanceMeters,
    null
)

fun testBikeRouteModel(
        boundingBox: BoundingBox = Mockito.mock(BoundingBox::class.java),
        departurePoint: RoutePoint = Mockito.mock(RoutePoint::class.java),
        destinationPoint: RoutePoint = Mockito.mock(RoutePoint::class.java)
) = BikeRouteModel(
        boundingBox,
        departurePoint,
        destinationPoint
)

fun testRouteSection(
    isWalk: Boolean = false,
    distance: String? = null,
    distanceValue: Double = 0.0,
    time: String? = null,
    departureTime: DateTime? = null,
    arrivalTime: DateTime? = null,
    polyline: Polyline = Mockito.mock(Polyline::class.java),
    subpolyline: Subpolyline = Mockito.mock(Subpolyline::class.java),
    transports: List<RouteModel.Transport> = emptyList(),
    routeStops: List<RouteModel.RouteStop> = emptyList()
) = RouteModel.RouteSection(
    isWalk,
    distance,
    distanceValue,
    time,
    departureTime,
    arrivalTime,
    polyline,
    subpolyline,
    transports,
    routeStops
)

fun testTransport(
    lineId: String = "",
    name: String = "",
    color: Int = 0,
    type: VehicleType = VehicleType.BUS,
    isNight: Boolean = false,
    isRecommended: Boolean = false,
    alternateDepartureStopId: String = "",
    alerts: List<ThreadAlert> = emptyList()
) = RouteModel.Transport(lineId, name, color, type, isNight, isRecommended, alternateDepartureStopId, alerts)

fun testRouteStop(
    stopId: String = "",
    name: String = "",
    position: Point = Mockito.mock(Point::class.java)
) = RouteModel.RouteStop(stopId, name, position)

fun testMasstransitRouteModel(transports: List<RouteModel.Transport>): MasstransitRouteModel {
    val sections = listOf(testRouteSection(transports = transports))
    return testMasstransitRouteModel(sections = sections)
}
