package ru.yandex.yandexbus.inhouse.route.routesetup

import com.yandex.mapkit.Time
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.yandex.yandexbus.inhouse.model.VehicleType
import ru.yandex.yandexbus.inhouse.model.alert.ClosedUntil
import ru.yandex.yandexbus.inhouse.model.route.MasstransitRouteModel
import ru.yandex.yandexbus.inhouse.repos.TimeLimitation
import java.util.concurrent.TimeUnit

// Test cases from
// https://wiki.yandex-team.ru/jandekskarty/programma-peshexodov/mobile-transport/Dizajjn-Transporta/Sortirovka-marshrutov-v-zavisimosti-ot-vremeni-sutok/

class RouteVariantsTest {

    @Test
    fun `there is a masstransit route and all the pedestrian routes are longer than 20 min`() {
        val masstransit = testMasstransitRouteModel()
        val taxi = testTaxiRouteModel()
        val pedestrian = longPedestrianRoute()
        val bike = testBikeRouteModel()

        val routeVariants = routeVariants(masstransit, pedestrian, taxi, bike)
        assertBlocksOrder(
            routeVariants,
            EtaBlock.Masstransit::class.java,
            EtaBlock.Bike::class.java,
            EtaBlock.Taxi::class.java,
            EtaBlock.Pedestrian::class.java
        )
    }

    @Test
    fun `there are masstransit route and filtered masstransit route`() {
        val masstransit = testMasstransitRouteModel()
        val filteredMasstransit = testMasstransitRouteModel(listOf(testTransport(type = VehicleType.MINIBUS)))

        val timeDependentResult = timeDependentResult(
            listOf(masstransit, filteredMasstransit),
            listOf(longPedestrianRoute()),
            listOf(testTaxiRouteModel())
        )
        val routeVariants = routeVariants(timeDependentResult, excludedTypes = setOf(VehicleType.MINIBUS))

        assertBlocksOrder(
            routeVariants,
            EtaBlock.Masstransit::class.java,
            EtaBlock.FilteredMasstransit::class.java,
            EtaBlock.Taxi::class.java,
            EtaBlock.Pedestrian::class.java
        )
    }

    @Test
    fun `there is a masstransit route and one of pedestrian routes are shorter than 20 min`() {
        val masstransit = testMasstransitRouteModel()
        val taxi = testTaxiRouteModel()
        val pedestrian = shortPedestrianRoute()
        val bike = testBikeRouteModel()

        val routeVariants = routeVariants(masstransit, pedestrian, taxi, bike)
        assertBlocksOrder(
            routeVariants,
            EtaBlock.Masstransit::class.java,
            EtaBlock.Bike::class.java,
            EtaBlock.Pedestrian::class.java,
            EtaBlock.Taxi::class.java
        )
    }

    @Test
    fun `there is no masstransit route and one of pedestrian routes are shorter than 20 min`() {
        val taxi = testTaxiRouteModel()
        val pedestrian = shortPedestrianRoute()

        val routeVariants =
            routeVariants(timeDependentResult(pedestrianRoutes = listOf(pedestrian), taxiRoutes = listOf(taxi)))
        assertBlocksOrder(
            routeVariants,
            EtaBlock.Pedestrian::class.java,
            EtaBlock.Taxi::class.java
        )
    }

    @Test
    fun `there is no masstransit route and all of the pedestrian routes are longer than 20 min`() {
        val taxi = testTaxiRouteModel()
        val pedestrian = longPedestrianRoute()

        val routeVariants =
            routeVariants(timeDependentResult(pedestrianRoutes = listOf(pedestrian), taxiRoutes = listOf(taxi)))
        assertBlocksOrder(
            routeVariants,
            EtaBlock.Taxi::class.java,
            EtaBlock.Pedestrian::class.java
        )
    }

    @Test
    fun `there is no masstransit route and there is a filtered masstransit route`() {
        val filteredMasstransit = testMasstransitRouteModel(listOf(testTransport(type = VehicleType.MINIBUS)))

        val timeDependentResult = timeDependentResult(
            listOf(filteredMasstransit),
            listOf(shortPedestrianRoute()),
            listOf(testTaxiRouteModel())
        )
        val routeVariants = routeVariants(timeDependentResult, excludedTypes = setOf(VehicleType.MINIBUS))

        assertBlocksOrder(
            routeVariants,
            EtaBlock.FilteredMasstransit::class.java,
            EtaBlock.Pedestrian::class.java,
            EtaBlock.Taxi::class.java
        )
    }

    @Test
    fun `there are no masstransit and pedestrian routes`() {
        val taxi = testTaxiRouteModel()

        val routeVariants = routeVariants(timeDependentResult(taxiRoutes = listOf(taxi)))
        assertBlocksOrder(
            routeVariants,
            EtaBlock.Taxi::class.java
        )
    }

    @Test
    fun `there is no operating masstransit route for now and all pedestrian routes are longer than 20 min`() {
        val notCurrentlyOperatingMasstransit = notOperatingNowMasstransitRoute()

        val timeDependentResult = timeDependentResult(
            listOf(notCurrentlyOperatingMasstransit),
            listOf(longPedestrianRoute()),
            listOf(testTaxiRouteModel())
        )

        val routeVariants = routeVariants(
            timeDependentResult,
            setOf(VehicleType.MINIBUS),
            TimeLimitation.createDepartureTime(0, true)
        )

        assertBlocksOrder(
            routeVariants,
            EtaBlock.Taxi::class.java,
            EtaBlock.Pedestrian::class.java,
            EtaBlock.NotOperatingNowMasstransit::class.java
        )
    }

    @Test
    fun `there is no operating masstransit route for now and there is a filtered route`() {
        val notCurrentlyOperatingMasstransit = notOperatingNowMasstransitRoute()
        val notCurrentlyOperatingFilteredMasstransit = notOperatingNowMasstransitRoute(VehicleType.MINIBUS)

        val timeDependentResult = timeDependentResult(
            listOf(notCurrentlyOperatingMasstransit, notCurrentlyOperatingFilteredMasstransit),
            listOf(longPedestrianRoute()),
            listOf(testTaxiRouteModel())
        )

        val routeVariants = routeVariants(
            timeDependentResult,
            setOf(VehicleType.MINIBUS),
            TimeLimitation.createDepartureTime(0, true)
        )

        assertBlocksOrder(
            routeVariants,
            EtaBlock.Taxi::class.java,
            EtaBlock.Pedestrian::class.java,
            EtaBlock.NotOperatingNowMasstransit::class.java,
            EtaBlock.NotOperatingNowFilteredMasstransit::class.java
        )
    }

    @Test
    fun `there is masstransit route and not currently operating masstransit route`() {
        val masstransitRoute = testMasstransitRouteModel()
        val notCurrentlyOperatingMasstransit = notOperatingNowMasstransitRoute()

        val timeDependentResult = timeDependentResult(
            listOf(masstransitRoute, notCurrentlyOperatingMasstransit),
            listOf(longPedestrianRoute()),
            listOf(testTaxiRouteModel())
        )

        val routeVariants = routeVariants(
            timeDependentResult = timeDependentResult,
            timeLimitation = TimeLimitation.createDepartureTime(0, true)
        )

        assertBlocksOrder(
            routeVariants,
            EtaBlock.Masstransit::class.java,
            EtaBlock.Taxi::class.java,
            EtaBlock.Pedestrian::class.java,
            EtaBlock.NotOperatingNowMasstransit::class.java
        )
    }
}

private fun assertBlocksOrder(routeVariants: RouteVariants, vararg types: Class<out EtaBlock<*>>) {
    val sortedBlocks = routeVariants.sortedBlocks
    val errorMessage = "Incorrect order ${sortedBlocks.map { it::class.java.simpleName }}. Should be: ${types.map { it.simpleName }}"

    assertEquals(types.size, sortedBlocks.size)
    types.withIndex().forEach { value ->
        assertTrue(errorMessage, sortedBlocks[value.index]::class.java == value.value)
    }
}

private fun shortPedestrianRoute() = pedestrianRoute(15, TimeUnit.MINUTES)
private fun longPedestrianRoute() = pedestrianRoute(5, TimeUnit.HOURS)
private fun pedestrianRoute(travelTime: Long, timeUnit: TimeUnit) =
    testPedestrianRouteModel(travelTimeSeconds = timeUnit.toSeconds(travelTime).toDouble())

private fun notOperatingNowMasstransitRoute(transportType: VehicleType = VehicleType.BUS): MasstransitRouteModel {
    // Alert opens in 3 hours
    val alertOpenTime = TimeUnit.HOURS.toSeconds(3)
    val alert = ClosedUntil("", Time(alertOpenTime, 0, ""))

    val transports = listOf(testTransport(isRecommended = true, alerts = listOf(alert), type = transportType))
    return testMasstransitRouteModel(transports)
}
