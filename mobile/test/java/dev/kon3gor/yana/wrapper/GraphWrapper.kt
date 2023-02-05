package dev.kon3gor.yana.wrapper

import dev.kon3gor.yana.dsl.graph.graph
import dev.kon3gor.yana.dsl.subGraph.subGraph
import dev.kon3gor.yana.mocks.Node

class GraphWrapper(
    private val start: Wrapper,
    private val children: List<Wrapper> = listOf(),
) : Wrapper() {

    override val id = generateId()

    override fun build() = subGraph<Node>(id, start.build()).apply {
        children.forEach { include(it.build()) }
    }

    fun buildMain() = graph(id, start.build()).apply {
        children.forEach { include(it.build()) }
    }

}