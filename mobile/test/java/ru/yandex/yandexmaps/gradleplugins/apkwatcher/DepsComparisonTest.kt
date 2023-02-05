package ru.yandex.yandexmaps.gradleplugins.apkwatcher

import ru.yandex.yandexmaps.gradleplugins.apkwatcher.deps.DepsComparator
import ru.yandex.yandexmaps.gradleplugins.apkwatcher.deps.DepsComparisonResult
import ru.yandex.yandexmaps.gradleplugins.apkwatcher.deps.DepsEntry
import ru.yandex.yandexmaps.gradleplugins.apkwatcher.deps.UpdatedDepsEntry
import kotlin.test.Test
import kotlin.test.assertEquals

class DepsComparisonTest {

    private val testingSubject = DepsComparator

    @Test
    fun `compare deps`() {
        val current = listOf(
            DepsEntry("test-group", "test1", "0.0.2"),
            DepsEntry("test-group", "test2", "0.0.2"),
            DepsEntry("test-group", "test3", "0.0.3")
        )
        val reference = listOf(
            DepsEntry("test-group", "test1", "0.0.1"),
            DepsEntry("test-group", "test2", "0.0.2")
        )
        val expected = DepsComparisonResult(
            listOf(
                DepsEntry("test-group", "test3", "0.0.3")
            ),
            listOf(
                UpdatedDepsEntry(DepsEntry("test-group", "test1", "0.0.1"), DepsEntry("test-group", "test1", "0.0.2"))
            )
        )

        val actual = testingSubject.compareDeps(current, reference)

        assertEquals(expected, actual)
    }

    @Test
    fun `compare deps no new deps`() {
        val current = listOf(
            DepsEntry("test-group", "test1", "0.0.1"),
            DepsEntry("test-group", "test2", "0.0.2"),
        )
        val reference = listOf(
            DepsEntry("test-group", "test1", "0.0.1"),
            DepsEntry("test-group", "test2", "0.0.2"),
            DepsEntry("test-group", "test3", "0.0.3")
        )
        val expected = DepsComparisonResult(listOf(), listOf())

        val actual = testingSubject.compareDeps(current, reference)

        assertEquals(expected, actual)
    }
}
