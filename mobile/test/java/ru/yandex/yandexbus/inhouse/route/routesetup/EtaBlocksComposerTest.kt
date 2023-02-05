package ru.yandex.yandexbus.inhouse.route.routesetup

import com.yandex.mapkit.Time
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import ru.yandex.yandexbus.inhouse.model.VehicleType
import ru.yandex.yandexbus.inhouse.model.alert.Closed
import ru.yandex.yandexbus.inhouse.model.alert.ClosedUntil
import ru.yandex.yandexbus.inhouse.model.alert.UnclassifiedAlert
import ru.yandex.yandexbus.inhouse.repos.TimeLimitation
import ru.yandex.yandexbus.inhouse.utils.datetime.DateFormat
import java.util.concurrent.TimeUnit

class EtaBlocksComposerTest {

    @Test
    fun emptyRouteVariantsWhenNoRoutes() {
        val routeVariants = routeVariants(timeDependentResult())
        assertFalse(routeVariants.hasRoutes)
    }

    @Test
    fun containsMasstransitBlock() {
        val timeDependentResult = timeDependentResult(testMasstransitRouteModel())
        val routeVariants = routeVariants(timeDependentResult)
        assertConsistsOfTypes(routeVariants, EtaBlock.Masstransit::class.java)
    }

    @Test
    fun containsFilteredMasstransitBlock() {
        val transports = listOf(testTransport(type = VehicleType.MINIBUS))
        val routeModel = testMasstransitRouteModel(transports)

        val timeDependentResult = timeDependentResult(routeModel)
        val routeVariants = routeVariants(timeDependentResult, excludedTypes = setOf(VehicleType.MINIBUS))

        assertConsistsOfTypes(routeVariants, EtaBlock.FilteredMasstransit::class.java)
    }

    @Test
    fun containsPedestrianBlock() {
        val timeDependentResult = timeDependentResult(testPedestrianRouteModel())
        val routeVariants = routeVariants(timeDependentResult)

        assertConsistsOfTypes(routeVariants, EtaBlock.Pedestrian::class.java)
    }

    @Test
    fun containsTaxiBlock() {
        val timeDependentResult = timeDependentResult(testTaxiRouteModel())
        val routeVariants = routeVariants(timeDependentResult)

        assertConsistsOfTypes(routeVariants, EtaBlock.Taxi::class.java)
    }

    @Test
    fun `contains OtherBlock when recommended transport has Closed alert`() {
        val alerts = listOf(Closed("", mock(Time::class.java)))
        val transports = listOf(testTransport(isRecommended = true, alerts = alerts))
        val routeModel = testMasstransitRouteModel(transports)

        val timeDependentResult = timeDependentResult(routeModel)
        val routeVariants =
            routeVariants(timeDependentResult, timeLimitation = TimeLimitation.createDepartureTime(0, true));

        assertConsistsOfTypes(routeVariants, EtaBlock.NotOperatingNowMasstransit::class.java)
    }

    @Test
    fun `contains OtherBlock when recommended transport has ClosedUntil Alert that will not be opened soon`() {
        val currentTime = 0L
        // Alert opens in 3 hours
        val alertOpenTime = TimeUnit.HOURS.toSeconds(3)
        val alert = ClosedUntil("", Time(alertOpenTime, 0, ""))

        val transports = listOf(testTransport(isRecommended = true, alerts = listOf(alert)))
        val routeModel = testMasstransitRouteModel(transports)

        val timeDependentResult = timeDependentResult(routeModel)
        val routeVariants =
            routeVariants(timeDependentResult, timeLimitation = TimeLimitation.createDepartureTime(currentTime, true));

        assertConsistsOfTypes(routeVariants, EtaBlock.NotOperatingNowMasstransit::class.java)
    }

    @Test
    fun `does not contain other block when recommended transport has ClosedUntil alert that will be opened soon`() {
        val currentTime = 0L
        // Alert opens in 15 min
        val alertOpenTime = TimeUnit.MINUTES.toSeconds(15)
        val alert = ClosedUntil("", Time(alertOpenTime, 0, ""))

        val transports = listOf(testTransport(isRecommended = true, alerts = listOf(alert)))
        val routeModel = testMasstransitRouteModel(transports)

        val timeDependentResult = timeDependentResult(routeModel)
        val routeVariants =
            routeVariants(timeDependentResult, timeLimitation = TimeLimitation.createDepartureTime(currentTime, true));

        assertConsistsOfTypes(routeVariants, EtaBlock.Masstransit::class.java)
    }

    @Test
    fun `does not contain other block when recommended transport does not have Closed or ClosedUntil alerts`() {
        val alert = UnclassifiedAlert("")

        val transports = listOf(testTransport(isRecommended = true, alerts = listOf(alert)))
        val routeModel = testMasstransitRouteModel(transports)

        val timeDependentResult = timeDependentResult(routeModel)
        val routeVariants =
            routeVariants(timeDependentResult, timeLimitation = TimeLimitation.createDepartureTime(0, true));

        assertConsistsOfTypes(routeVariants, EtaBlock.Masstransit::class.java)
    }

    @Test
    fun `does not contain other block when TimeLimitation is Arrival`() {
        val currentTime = 0L
        // Alert opens in 3 hours
        val alertOpenTime = TimeUnit.HOURS.toSeconds(3)
        val alert = ClosedUntil("", Time(alertOpenTime, 0, ""))

        val transports = listOf(testTransport(isRecommended = true, alerts = listOf(alert)))
        val routeModel = testMasstransitRouteModel(transports)

        val timeDependentResult = timeDependentResult(routeModel)
        val routeVariants =
            routeVariants(timeDependentResult, timeLimitation = TimeLimitation.createArrivalTime(currentTime));

        assertConsistsOfTypes(routeVariants, EtaBlock.Masstransit::class.java)
    }

    @Test
    fun routesAreGropedIntoBlocks() {
        val masstransit = listOf(testMasstransitRouteModel(), testMasstransitRouteModel())
        val pedestrian = listOf(testPedestrianRouteModel(), testPedestrianRouteModel())
        val taxi = listOf(testTaxiRouteModel(), testTaxiRouteModel())

        val timeDependentResult = timeDependentResult(masstransit, pedestrian, taxi)
        val routeVariants = routeVariants(timeDependentResult)

        assertConsistsOfTypes(routeVariants, EtaBlock.Masstransit::class.java, EtaBlock.Pedestrian::class.java, EtaBlock.Taxi::class.java)
        routeVariants.sortedBlocks.forEach {
            assertEquals(2, it.routes.size)
        }
    }

    @Test
    fun routesAreSortedByArrivalEstimationInsideBlock() {
        val testDateFormat = DateFormat("dd.MM.yyyy HH:mm")

        val masstransit = listOf(
            testMasstransitRouteModel(arrivalEstimation = testDateFormat.parse("23.07.1997 15:20")),
            testMasstransitRouteModel(arrivalEstimation = null),
            testMasstransitRouteModel(arrivalEstimation = testDateFormat.parse("23.07.1997 11:00")),
            testMasstransitRouteModel(arrivalEstimation = null)
        )

        val routeVariants = routeVariants(timeDependentResult(masstransit))

        assertConsistsOfTypes(routeVariants, EtaBlock.Masstransit::class.java)

        val routes = routeVariants.sortedBlocks.first().routes
        assertEquals(4, routes.size)

        val expectedArrivalEstimations = listOf<String?>(
            null,
            null,
            "23.07.1997 11:00",
            "23.07.1997 15:20"
        )

        expectedArrivalEstimations.withIndex()
            .forEach { (index, value) ->
                val expectedArrivalEstimation = value?.let { testDateFormat.parse(it) }
                assertEquals(expectedArrivalEstimation?.localMillis, routes[index].arrivalEstimation?.localMillis)
            }
    }
}

private fun <T : EtaBlock<*>> assertConsistsOfTypes(routeVariants: RouteVariants, vararg types: Class<out T>) {
    assertEquals(types.size, routeVariants.sortedBlocks.size)
    assertTrue(routeVariants.sortedBlocks.asSequence().map { it::class.java }.toSet() == types.toSet())
}