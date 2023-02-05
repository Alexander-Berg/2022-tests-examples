package dev.kon3gor.yana

import dev.kon3gor.podcasty.navigation.mocks.*
import dev.kon3gor.yana.dsl.base.NavigationContext
import org.junit.Assert.*
import org.junit.Test

class NodeFinderTest {

    @Test
    fun `find node with depth 0`() {
        val finder = NodeFinder(mainGraph.buildMain(), mock7.build())
        val result = finder.findNode().map { it.id }
        assertEquals(listOf(mock7.id), result)
    }

    @Test
    fun `find node with depth 1`() {
        val finder = NodeFinder(mainGraph.buildMain(), mock1.build())
        val result = finder.findNode().map { it.id }
        assertEquals(listOf(subGraph2.id, mock1.id), result)
    }

    @Test
    fun `find node with depth 2`() {
        val finder = NodeFinder(mainGraph.buildMain(), mock4.build())
        val result = finder.findNode().map { it.id }
        assertEquals(listOf(subGraph3.id, subGraph4.id, mock4.id), result)
    }

    @Test
    fun `cannot find chain to isolated graph`() {
        val finder = NodeFinder(mainGraph2.buildMain(), mock1.build())
        val result = finder.findNode()
        assertEquals(emptyList<NavigationContext>(), result)
    }
}