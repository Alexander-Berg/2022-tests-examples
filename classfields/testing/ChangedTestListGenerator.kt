package com.yandex.mobile.realty.testing

import com.yandex.mobile.realty.testing.shell.shellRun
import com.yandex.mobile.realty.testing.shell.ArcCommand.Companion.TRUNK_BRANCH
import com.yandex.mobile.realty.testing.shell.DiffFiles
import java.io.File

class ChangedTestListGenerator {

    fun generate(): List<String> {
        return shellRun {
            val branch = arc.getCurrentBranchName()
            val commit = arc.mergeBase(branch, TRUNK_BRANCH)
            arc.diffFiles(commit, branch)
        }
            .getTestFiles()
            .getChangedTestsForRun()
    }

    private fun List<DiffFiles.DiffFile>.getChangedTestsForRun(): List<String> =
        filter { it.isNewFile() || it.isModified() }.map { diffFile ->
            val lines = File(diffFile.getPath()).readLines()
            lines.mapIndexedNotNull { index, line ->
                if (CLASS_NAME_REGEX.matches(line) && lines.getAllAnnotation(index).any { it.hasAnnotationRunWith() }) {
                    CLASS_NAME_REGEX.find(line)?.groups?.get(2)?.value
                } else null
            }.formatForTestRun(lines.getPackage())
        }.flatten()

    private fun List<String>.getAllAnnotation(index: Int): List<String> {
        return mutableListOf<String>().also { annotationList ->
            var i = index
            do {
                i--
                val annotation = this.getNextAnnotation(i)
                annotation?.let { annotationList.add(it) }
            } while (annotation != null)
        }
    }

    private fun List<String>.getNextAnnotation(index: Int): String? {
        val nextAnnotation = this.getOrNull(index) ?: return null
        return if (ANNOTATION_REGEX.matches(nextAnnotation)) {
            ANNOTATION_REGEX.find(nextAnnotation)?.groups?.get(1)?.value
        } else null
    }

    private fun List<DiffFiles>.getTestFiles() = map { it.names }.flatten()
        .filter { diff -> TEST_DIRS.any { diff.path.contains(it) } }

    private fun DiffFiles.DiffFile.getPath() = path.replace(WORK_DIR_FROM_ARCADIA, "")

    private fun String.hasAnnotationRunWith() = contains(ANNOTATION_RUN_WITH)

    private fun List<String>.getPackage(): String = firstOrNull { PACKAGE_REGEX.matches(it) }?.let { packageLine ->
        PACKAGE_REGEX.find(packageLine)?.groups?.get(1)?.value
    } ?: ""

    private fun List<String>.formatForTestRun(testPackage: String): List<String> =
        map { testClassName -> "$testPackage$NEW_TEST_PATH_SEPARATOR$testClassName" }

    companion object {
        private const val WORK_DIR_FROM_ARCADIA = "classifieds/mobile-autoru-client-android/"
        private val TEST_DIRS = listOf(
            "/app/src/androidTest/java/ru/auto/ara/test/",
            "/app/src/androidTest/java/ru/auto/ara/screenshotTests/"
        )

        private const val ANNOTATION_RUN_WITH = "@RunWith"

        private const val NEW_TEST_PATH_SEPARATOR = "."

        private val CLASS_NAME_REGEX = "[\\s|\\r|\\t]*(class\\s+)([\\w\\d]+)(.*)".toRegex()
        private val ANNOTATION_REGEX = "[\\s|\\r|\\t]*(@.+)".toRegex()
        private val PACKAGE_REGEX = "package\\s(.+)".toRegex()
    }
}
