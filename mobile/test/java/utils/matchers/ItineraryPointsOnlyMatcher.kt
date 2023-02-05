package utils.matchers

import org.mockito.ArgumentMatcher
import ru.yandex.yandexmaps.multiplatform.core.geometry.Point
import ru.yandex.yandexmaps.multiplatform.routescommon.Itinerary
import ru.yandex.yandexmaps.multiplatform.routescommon.SteadyWaypoint

class ItineraryPointsOnlyMatcher(private val points: List<Point>, private val epsilon: Float) : ArgumentMatcher<Itinerary> {

    override fun matches(itinerary: Itinerary): Boolean {
        return itinerary.waypoints
            .filterIsInstance<SteadyWaypoint>()
            .map { it.point }
            .zip(points, ::samePoints)
            .all { it }
    }

    override fun toString(): String {
        return points.toString()
    }

    private fun samePoints(point1: Point, point2: Point): Boolean {
        return Math.abs(point1.lat - point2.lat) < epsilon && Math.abs(point1.lon - point2.lon) < epsilon
    }
}
