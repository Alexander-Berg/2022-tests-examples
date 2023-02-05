package ru.yandex.yandexbus.inhouse.route.routesetup

import org.mockito.Mockito
import ru.yandex.yandexbus.inhouse.model.Hotspot
import ru.yandex.yandexbus.inhouse.model.VehicleType
import ru.yandex.yandexbus.inhouse.model.route.BikeRouteModel
import ru.yandex.yandexbus.inhouse.model.route.MasstransitRouteModel
import ru.yandex.yandexbus.inhouse.model.route.PedestrianRouteModel
import ru.yandex.yandexbus.inhouse.model.route.RouteModel
import ru.yandex.yandexbus.inhouse.model.route.TaxiRouteModel
import ru.yandex.yandexbus.inhouse.repos.TimeLimitation

fun routeVariants(
    masstransit: MasstransitRouteModel, pedestrian: PedestrianRouteModel, taxi: TaxiRouteModel, bike: BikeRouteModel
) = routeVariants(
    timeDependentResult(
        listOf(masstransit),
        listOf(pedestrian),
        listOf(taxi),
        listOf(bike)
    )
)

fun timeDependentResult(
    masstransitRoutes: List<MasstransitRouteModel> = emptyList(),
    pedestrianRoutes: List<PedestrianRouteModel> = emptyList(),
    taxiRoutes: List<TaxiRouteModel> = emptyList(),
    bikeRoutes: List<BikeRouteModel> = emptyList()
) = RouteBuildUseCase.TimeDependentResult(masstransitRoutes.mapWithHotspot(), pedestrianRoutes, taxiRoutes, bikeRoutes)

fun routeVariants(
    timeDependentResult: RouteBuildUseCase.TimeDependentResult = timeDependentResult(),
    excludedTypes: Set<VehicleType> = emptySet(),
    timeLimitation: TimeLimitation = TimeLimitation.departureNow()
) = EtaBlocksComposer.createRouteVariants(
    timeDependentResult,
    excludedTypes,
    timeLimitation
)

fun <T : RouteModel> timeDependentResult(routeModel: T): RouteBuildUseCase.TimeDependentResult {
    return when (routeModel) {
        is MasstransitRouteModel -> timeDependentResult(masstransitRoutes = listOf(routeModel as MasstransitRouteModel))
        is PedestrianRouteModel -> timeDependentResult(pedestrianRoutes = listOf(routeModel as PedestrianRouteModel))
        is TaxiRouteModel -> timeDependentResult(taxiRoutes = listOf(routeModel as TaxiRouteModel))
        is BikeRouteModel -> timeDependentResult(bikeRoutes = listOf(routeModel as BikeRouteModel))
        else -> throw IllegalStateException("Unknown route model type")
    }
}

fun List<MasstransitRouteModel>.mapWithHotspot() = associate { it to Mockito.mock(Hotspot::class.java) }
