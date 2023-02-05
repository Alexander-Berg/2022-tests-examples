package dev.kon3gor.podcasty.navigation.mocks

import dev.kon3gor.yana.wrapper.GraphWrapper

val subGraph5 = GraphWrapper(mock6)

val subGraph4 = GraphWrapper(mock4, listOf(mock5, subGraph5))

val subGraph3 = GraphWrapper(subGraph4, listOf(mock3))

val subGraph2 = GraphWrapper(mock1, listOf(mock2))

val mainGraph = GraphWrapper(subGraph2, listOf(subGraph3, mock7))

val subGraph6 = GraphWrapper(mock8)

val mainGraph2 = GraphWrapper(subGraph6)