package ru.yandex.yandexmaps.gradleplugins.apkwatcher

import ru.yandex.yandexmaps.gradleplugins.apkwatcher.deps.DepsComparisonResult
import ru.yandex.yandexmaps.gradleplugins.apkwatcher.deps.DepsEntry
import ru.yandex.yandexmaps.gradleplugins.apkwatcher.deps.UpdatedDepsEntry
import ru.yandex.yandexmaps.gradleplugins.apkwatcher.reporter.BuildInfo
import ru.yandex.yandexmaps.gradleplugins.apkwatcher.reporter.CommentComposer
import ru.yandex.yandexmaps.gradleplugins.apkwatcher.reporter.SizeInfo
import kotlin.test.Test
import kotlin.test.assertEquals

class CommentComposerTest {

    private val testingSubject = CommentComposer()

    @Test
    fun `create deps comment`() {
        val depsComparisonResult = DepsComparisonResult(
            listOf(
                DepsEntry("test-group", "test1", "0.0.1"),
                DepsEntry("test-group", "test2", "0.0.2"),
                DepsEntry("test-group", "test3", "0.0.3")
            ),
            listOf(UpdatedDepsEntry(DepsEntry("test-group", "test4", "0.0.3"), DepsEntry("test-group", "test4", "0.0.4")))
        )
        val buildInfo = BuildInfo("20000", "10000")

        val expected = """
            ಠ_ಠ
            There are changes in build #20000 compared to #10000
            Some dependencies have changed!
            New deps:
            ```
            test-group:test1:0.0.1
            test-group:test2:0.0.2
            test-group:test3:0.0.3
            ```
            Updated deps:
            ```
            test-group:test4:0.0.3 -> 0.0.4
            ```
        """.trimIndent()

        val actual = testingSubject.createDepsComment(depsComparisonResult, buildInfo)

        assertEquals(expected, actual)
    }

    @Test
    fun `create size comment`() {
        val sizeInfo = SizeInfo(3000000, 6000000)
        val buildInfo = BuildInfo("20000", "10000")

        val expected = """
            ಠ_ಠ
            There are changes in build #20000 compared to #10000
            Significant apk size change detected!
            Size is 5.72 MB compared to 2.86 MB in trunk.
        """.trimIndent()

        val actual = testingSubject.createSizeComment(sizeInfo, buildInfo)

        assertEquals(expected, actual)
    }

    @Test
    fun `create comment with prefix`() {
        val composerWithPrefix = CommentComposer("maps")

        val depsComparisonResult = DepsComparisonResult(
            listOf(
                DepsEntry("test-group", "test1", "0.0.1"),
            ),
            emptyList(),
        )

        val expected = """
            ಠ_ಠ
            Some dependencies in maps have changed!
            New deps:
            ```
            test-group:test1:0.0.1
            ```
            @someone, @someone_else
        """.trimIndent()

        val actual = composerWithPrefix.createDepsComment(depsComparisonResult, mentions = listOf("someone", "someone_else"))

        assertEquals(expected, actual)
    }
}
