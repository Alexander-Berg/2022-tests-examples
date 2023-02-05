package ru.yandex.yandexmaps.gradleplugins.apkwatcher

import ru.yandex.yandexmaps.gradleplugins.apkwatcher.utils.findPaths
import ru.yandex.yandexmaps.gradleplugins.apkwatcher.utils.findSinglePath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class FindPathsTest {
    private val getChildren: (String) -> List<String>
    init {
        val childrenMap = mapOf(
            "" to listOf("a"),
            "a" to listOf("b", "c"),
        )
        getChildren = { path -> childrenMap.getOrDefault(path, listOf()) }
    }

    @Test
    fun `find paths`() {
        assertEquals(listOf("a"), findPaths("a", getChildren))
        assertEquals(listOf("a"), findPaths("*", getChildren))

        assertEquals(listOf("a/b"), findPaths("a/b", getChildren))
        assertEquals(listOf("a/b"), findPaths("*/b", getChildren))
        assertEquals(listOf("a/b", "a/c"), findPaths("a/*", getChildren))
        assertEquals(listOf("a/b", "a/c"), findPaths("*/*", getChildren))

        assertEquals(listOf(), findPaths("a/d", getChildren))
        assertEquals(listOf(), findPaths("a/d/d", getChildren))
        assertEquals(listOf(), findPaths("*/*/*", getChildren))
    }

    @Test
    fun `find single path`() {
        assertEquals("a/c", findSinglePath("a/c", getChildren))
        assertEquals("a/c", findSinglePath("*/c", getChildren))
    }

    @Test
    fun `find single path no results`() {
        val exception = assertFails { findSinglePath("a/d", getChildren) }
        assertEquals("Didn't find any files for pattern 'a/d'", exception.message)
    }

    @Test
    fun `find single paths multiple results`() {
        val exception = assertFails { findSinglePath("*/*", getChildren) }
        assertTrue(exception.message!!.startsWith("Found more than 1 file for pattern '*/*': "))
    }
}
