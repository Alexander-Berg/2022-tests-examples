package ru.yandex.yandexmaps.tools.testpalm.impl

import ru.yandex.yandexmaps.tools.testpalm.impl.formatter.TestCaseFormatter
import java.io.File

class FileProcessor(
    private val projectId: String,
    private val testCasesDir: String,
    private val fileNameFormat: String,
    private val testCaseFormatter: TestCaseFormatter,
) {
    private val oldSuffix = "_old"

    fun fileExists(testCase: TestCase): Boolean = file(testCase).exists()

    fun createFile(testCase: TestCase, replaceOld: Boolean): Pair<String, String?> {
        val f = file(testCase)
        var oldName: String? = null
        if (replaceOld && f.exists()) {
            val oldFile = file(testCase, true)
            f.copyTo(oldFile, true)
            f.delete()
            oldName = oldFile.name
        }
        val new = file(testCase)
        new.createNewFile()
        new.writeText(testCaseFileText(testCase))
        return f.name to oldName
    }

    fun getHashFromFile(testCase: TestCase): Int? =
        file(testCase).readText().split("\n").firstOrNull()?.let { testCaseFormatter.getHash(it) }

    fun getAllTestcaseFiles(): List<Pair<String, String>> =
        (File(testCasesDir).list() ?: emptyArray()).mapNotNull { name -> testCaseName(name)?.let { it to name } }

    fun getDuplicatedTestcaseFiles(files: List<Pair<String, String>>): List<Pair<String, String>> =
        files.filter { it.component2().contains(oldSuffix) }

    private fun testCaseName(fileName: String): String? {
        val filePrefix = fileNameFormat.split("%s").firstOrNull() ?: ""
        val filePostfix = fileNameFormat.split("%s").lastOrNull() ?: ""
        if (fileName.startsWith(filePrefix) && fileName.endsWith(filePostfix)) {
            val tcName = fileName.removePrefix(filePrefix).removeSuffix(filePostfix)
            val projectPrefix = "$projectId-"
            if (tcName.startsWith(projectPrefix) && tcName.removePrefix(projectPrefix).removeSuffix(oldSuffix).toIntOrNull() != null) {
                return tcName
            }
        }
        return null
    }

    private fun file(testCase: TestCase, old: Boolean = false): File {
        val filePostfix = if (old) oldSuffix else ""
        return File("$testCasesDir/${fileNameFormat.format(testCase.fullName + filePostfix)}")
    }

    private fun testCaseFileText(testCase: TestCase): String =
        "${testCaseFormatter.printHash(testCase.hashCode())}\n${testCaseFormatter.printTestCase(testCase)}"
}
