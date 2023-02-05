package ru.yandex.yandexmaps.roulette.test

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.yandex.yandexmaps.multiplatform.core.geometry.EPSILON_GEO
import ru.yandex.yandexmaps.multiplatform.core.geometry.Point
import ru.yandex.yandexmaps.multiplatform.core.utils.DebugLog
import ru.yandex.yandexmaps.multiplatform.mapkit.extensions.toCommonPoint
import ru.yandex.yandexmaps.multiplatform.mapkit.geometry.NativePoint
import ru.yandex.yandexmaps.multiplatform.mapkit.map.CameraPosition
import ru.yandex.yandexmaps.multiplatform.mapkit.utils.Geo
import ru.yandex.yandexmaps.multiplatform.redux.api.Action
import ru.yandex.yandexmaps.roulette.internal.redux.AddLandmark
import ru.yandex.yandexmaps.roulette.internal.redux.AnalyticsNonReduceAction
import ru.yandex.yandexmaps.roulette.internal.redux.LogDragFinished
import ru.yandex.yandexmaps.roulette.internal.redux.LogMapCenter
import ru.yandex.yandexmaps.roulette.internal.redux.MoveLandmark
import ru.yandex.yandexmaps.roulette.internal.redux.RemoveLandmark
import ru.yandex.yandexmaps.roulette.internal.redux.RemoveLastLandmark
import ru.yandex.yandexmaps.roulette.internal.redux.RouletteHintState
import ru.yandex.yandexmaps.roulette.internal.redux.RouletteLandmark
import ru.yandex.yandexmaps.roulette.internal.redux.RouletteState
import ru.yandex.yandexmaps.roulette.internal.redux.reduce
import ru.yandex.yandexmaps.roulette.internal.redux.totalDistance
import kotlin.math.sqrt

class ReducerTest {
    private val eps = EPSILON_GEO.toDouble()

    @Before
    fun setUp() {
        mockkObject(Geo, DebugLog)
        val from = slot<NativePoint>()
        val to = slot<NativePoint>()
        every {
            Geo.distance(capture(from), capture(to))
        } answers {
            distance(from.captured.toCommonPoint(), to.captured.toCommonPoint())
        }
    }

    @After
    fun tearDown() {
        unmockkObject(Geo, DebugLog)
    }

    @Test
    fun addLandmark_to_emptyState_leadsTo_StateWith1Landmark() {
        val state = RouletteState()
        val point = Point(10.0, 20.0)
        val reduced = state.reduce(AddLandmark(point))
        assertEquals(reduced, RouletteState(listOf(RouletteLandmark(point, id = 0))))
        assertEquals(.0, reduced.totalDistance, .0)
    }

    @Test
    fun addLandmark_toNotEmptyState_leadsToAddingLandmark() {
        var distance = 0.0
        val points = points()
        var state = RouletteState(listOf(RouletteLandmark(points[0], id = 0)))
        points.windowed(2, 1).forEachIndexed { index, (prev, current) ->
            distance += distance(prev, current)
            val prevState = state
            state = state.reduce(AddLandmark(current))
            assertEquals("pair index: $index", RouletteLandmark(current, distance, index + 1), state.landmarks.last())
            assertEquals("pair index: $index", prevState.landmarks, state.landmarks.dropLast(1))
            assertEquals("pair index: $index", distance, state.totalDistance, eps)
        }
    }

    @Test
    fun add_identicalLandmark_several_times_in_a_row_leadsToAddingOneLandmark() {
        val state = points().toState()
        val point = Point(10.0, 20.0)
        val identicalPoint = Point(lat = point.lat + EPSILON_GEO / 2, lon = point.lon - EPSILON_GEO / 2)
        val reduced = state
            .reduce(AddLandmark(point))
            .reduce(AddLandmark(point))
            .reduce(AddLandmark(identicalPoint))
            .reduce(AddLandmark(point))
        assertEquals((points() + point).toState(), reduced)
    }

    @Test
    fun add_identicalLandmark_several_times_not_in_a_row_leadsToAddingAll() {
        val state = points().toState()
        val point1 = Point(10.0, 20.0)
        val point2 = Point(200.0, 300.0)
        val reduced = state
            .reduce(AddLandmark(point1))
            .reduce(AddLandmark(point2))
            .reduce(AddLandmark(point1))
            .reduce(AddLandmark(point2))
        val expectedState = (points() + listOf(point1, point2, point1, point2)).toState()
        assertEquals(expectedState, reduced)
    }

    @Test
    fun removeLastLandmark_leadsTo_removingLandmark() {
        val points = points()
        val state = points().toState()
        var reduced = state

        (1..points.size).forEach { count ->
            reduced = reduced.reduce(RemoveLastLandmark)
            assertEquals("count: $count", state.landmarks.dropLast(count), reduced.landmarks)
            assertEquals("count: $count", points.dropLast(count).distance(), reduced.totalDistance, eps)
        }
    }

    @Test
    fun removeLandmark_leadsTo_tailDistancesRecomputing() {
        val points = points().toMutableList()
        val state = points.toState()

        val index = points.size / 2
        val reduced = state.reduce(RemoveLandmark(index))
        points.removeAt(index)

        assertEquals(points.toState().landmarks.last().distanceToStart, reduced.landmarks.last().distanceToStart, EPSILON_GEO.toDouble())
        assertEquals(state.landmarks.subList(0, index), reduced.landmarks.subList(0, index))
        assertEquals(points.size, reduced.landmarks.size)
    }

    @Test
    fun removeLandmark_notLeadsTo_idsRecomputuion() {
        val points = points().take(6)
        val expectedIds = listOf(1, 3, 4, 5) // skip 0, 2
        val reduced = points.toState().reduce(RemoveLandmark(2)).reduce(RemoveLandmark(0))

        assertEquals(expectedIds, reduced.landmarks.map { it.id })
    }

    @Test
    fun addOneLandmark_notLeadsTo_hintHidding() {
        val state = RouletteState()
        val reduced = state.reduce(AddLandmark(points().first()))
        assertEquals(RouletteHintState.Shown, reduced.hintState)
    }

    @Test fun addTwoLandmarks_leadsTo_hintHidding() {
        val state = RouletteState()
        val reduced = state.reduce(AddLandmark(points().first())).reduce(AddLandmark(points()[2]))
        assertEquals(RouletteHintState.Hidden, reduced.hintState)
    }

    @Test
    fun removeTwoLastLandmarks_notLeadsTo_hintStateShowing() {
        val points = points().take(2)
        val state = points.toState()
        var reduced = state.reduce(RemoveLastLandmark)
        assertEquals(RouletteHintState.Hidden, reduced.hintState)
        reduced = state.reduce(RemoveLastLandmark)
        assertEquals(RouletteHintState.Hidden, reduced.hintState)
    }

    @Test
    fun moveLandmark_leadsTo_tailRecomputing() {
        val points = points().toMutableList()
        var state = points.toState()

        points.indices.forEach { index ->
            val moved = Point(index.toDouble(), index.toDouble())
            val prevState = state
            state = state.reduce(MoveLandmark(index, moved))
            points[index] = moved

            assertEquals("index:$index", prevState.landmarks.subList(0, index), state.landmarks.subList(0, index))
            assertEquals("index:$index", prevState.landmarks.size, state.landmarks.size)
            assertEquals("index:$index", points.distance(), state.totalDistance, eps)
        }
    }

    @Test
    fun otherAction_leadsTo_sameState() {
        val state = points().toState()
        val reduced = state.reduce(object : Action {})
        assertEquals(state, reduced)
    }

    @Test
    fun analyticsActions_leadsTo_refEqualsState() {
        val state = points().toState()
        val reduced = state
            .reduce(LogMapCenter(mockk<CameraPosition>()))
            .reduce(LogDragFinished)
            .reduce(object : AnalyticsNonReduceAction {})

        assertTrue(state === reduced)
    }

    private fun distance(from: Point, to: Point) = sqrt(from.lat * to.lat + from.lon * to.lon)
    private fun points() = (0..10).map { Point(it.toDouble(), it.toDouble() * it) }
    private fun List<Point>.distance() = windowed(2, 1).fold(0.0) { total, (p1, p2) ->
        total + distance(p1, p2)
    }

    private fun List<Point>.toState() = RouletteState(
        fold(emptyList()) { acc, point ->
            val prev = acc.lastOrNull()
            acc + if (prev == null) {
                RouletteLandmark(point, id = acc.size)
            } else {
                RouletteLandmark(point, prev.distanceToStart + distance(point, prev.point), id = acc.size)
            }
        },
        hintState = if (size >= 2) RouletteHintState.Hidden else RouletteHintState.Shown
    )
}
