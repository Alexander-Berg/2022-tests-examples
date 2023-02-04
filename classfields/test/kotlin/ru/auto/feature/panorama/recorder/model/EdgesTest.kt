package ru.auto.feature.panorama.recorder.model

import io.qameta.allure.kotlin.junit4.AllureRunner
 import org.junit.Test
import org.junit.runner.RunWith
 import ru.auto.feature.panorama.core.model.Edge
import ru.auto.feature.panorama.core.model.EdgeWithTime
import kotlin.test.assertEquals

@RunWith(AllureRunner::class) class EdgesTest {

    @Test
    fun `reduce test`() {
        val edgesFlow = listOf(
            EdgeData(Edge.NONE, 0),
            EdgeData(Edge.FRONT, 0),
            EdgeData(Edge.FRONT, 200),
            EdgeData(Edge.FRONT, 400),
            EdgeData(Edge.NONE, 600),
            EdgeData(Edge.FRONT, 800),
            EdgeData(Edge.FRONT, 1000),
            EdgeData(Edge.FRONT_3_4, 1400),
            EdgeData(Edge.FRONT_3_4, 1400),
            EdgeData(Edge.FRONT_3_4, 1600),
            EdgeData(Edge.SIDE, 1800),
            EdgeData(Edge.SIDE, 2000),
            EdgeData(Edge.SIDE, 3200),
            EdgeData(Edge.SIDE, 3400)
        )

        val expected = edgesFlow.fold(Edges()) { edges, (edge, time) ->
            edges.reduce(edge, time)
        }

        val actual = Edges(
            listOf(
                Edges.EdgeRecord(edge = Edge.FRONT, startTime = 0, endTime = 1000, frameCount = 5),
                Edges.EdgeRecord(edge = Edge.FRONT_3_4, startTime = 1400, endTime = 1600, frameCount = 3),
                Edges.EdgeRecord(edge = Edge.SIDE, startTime = 1800, endTime = 2000, frameCount = 2),
                Edges.EdgeRecord(edge = Edge.SIDE, startTime = 3200, endTime = 3400, frameCount = 2)
            )
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `select test`() {

        val edges = Edges(
            listOf(
                // ok
                Edges.EdgeRecord(edge = Edge.FRONT, startTime = 0, endTime = 12_000, frameCount = 6),
                // repeated
                Edges.EdgeRecord(edge = Edge.FRONT, startTime = 12_100, endTime = 14_100, frameCount = 6),
                // ok
                Edges.EdgeRecord(edge = Edge.FRONT, startTime = 22_001, endTime = 33_000, frameCount = 6),
                // ok
                Edges.EdgeRecord(edge = Edge.SIDE, startTime = 33_000, endTime = 34_001, frameCount = 6),
                // too short
                Edges.EdgeRecord(edge = Edge.BACK, startTime = 33_000, endTime = 33_500, frameCount = 6)
            )
        )

        val expected = edges.select()

        val actual = listOf(
            EdgeWithTime(edge = Edge.FRONT, timeMs = 0),
            EdgeWithTime(edge = Edge.FRONT, timeMs = 6000),
            EdgeWithTime(edge = Edge.FRONT, timeMs = 27500),
            EdgeWithTime(edge = Edge.SIDE, timeMs = 33500)
        )

        assertEquals(expected, actual)
    }

    private data class EdgeData(val edge: Edge, val time: Long)

}
