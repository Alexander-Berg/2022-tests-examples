package dev.kon3gor.yana.wrapper

import dev.kon3gor.yana.dsl.base.NavigableGraphEntity
import kotlin.random.Random

abstract class Wrapper {
    abstract val id: Int

    abstract fun build(): NavigableGraphEntity

    fun generateId() = random.nextInt(0, Int.MAX_VALUE)

    companion object {
        protected val random = Random(42)
    }
}