package ru.yandex.yandexmaps.tools.testpalm.impl

import ru.yandex.yandexmaps.tools.testpalm.impl.formatter.MapsTestCaseFormatter

class ScenarioRunner(private val config: Config, private val token: String) {

    private val testPalmInteractor = TestPalmInteractorImpl(token, config.projectId, config.testPalmHost)
    private val fileProcessor = FileProcessor(config.projectId, config.testCasesDir, "%s.kt", MapsTestCaseFormatter)

    fun generateTestcaseTemplate(testCaseId: String, replace: Boolean) {
        val id = extractTestCaseNumber(testCaseId) ?: return
        val testCase = downloadTestCase(id) ?: return

        when (getTestCaseStatus(testCase)) {
            TestCaseStatus.UP_TO_DATE -> println("testcase ${testCase.fullName} implementation already exists and is up-to-date")
            TestCaseStatus.OUTDATED -> {
                if (!replace) {
                    println("testcase ${testCase.fullName} implementation already exists, but is outdated")
                    println("to replace old template use\n")
                    println("    testpalm generate [id] -f\n")
                } else {
                    createFile(testCase, replaceOld = true)
                }
            }
            TestCaseStatus.REMOTE -> createFile(testCase, replaceOld = false)
        }
    }

    fun checkLocalTestcases(testCaseId: String?) {
        val filter: ((String) -> Boolean)? = testCaseId?.let { extractTestCaseNumber(it) }?.let { id -> { it.contains("${config.projectId}-$id") } }
        val files = getTestcaseFiles(filter)

        if (files.isEmpty()) {
            println("no testcase files was found")
            return
        }

        val duplicated = fileProcessor.getDuplicatedTestcaseFiles(files)
        val filesToCheck = files.filter { !duplicated.contains(it) }

        if (duplicated.isNotEmpty()) {
            println("${duplicated.count()} testcase duplicates was found. please remove old testcase files")
            println(duplicated.joinToString("\n") { it.second })
            println()
        }

        if (filesToCheck.isEmpty()) {
            return
        }

        val testcaseIdsAndFiles = filesToCheck.mapNotNull {
            extractTestCaseNumber(it.first)?.let { id -> id to it.second }
        }
        println("downloading ${testcaseIdsAndFiles.count()} testcases")
        val testCases = testcaseIdsAndFiles.map { it.second to downloadTestCase(it.first, false) }

        val failedToLoad = testCases.mapNotNull { if (it.second == null) it.first else null }
        val loaded = testCases
            .mapNotNull { if (it.second != null) it.first to it.second!! else null }
            .map { Triple(it.first, it.second, getTestCaseStatus(it.second)) }

        val upToDate = loaded.filter { it.third == TestCaseStatus.UP_TO_DATE }
        val outdated = loaded.filter { it.third == TestCaseStatus.OUTDATED }

        println("Testcases: ${upToDate.count()} up-to-date; ${outdated.count()} outdated; ${failedToLoad.count()} failed to load")
        if (outdated.isNotEmpty()) {
            println("Outdated:")
            println(outdated.map { it.first }.joinToString("\n"))
            println()
        }

        if (failedToLoad.isNotEmpty()) {
            println("Failed to load:")
            println(failedToLoad.map { it }.joinToString("\n"))
        }
    }

    private fun extractTestCaseNumber(testCaseId: String): Int? {
        val components = testCaseId.split("-")
        val parsed: Int? = components.lastOrNull()?.let { it.toIntOrNull() }
        if (components.count() > 1) {
            val projId = components.dropLast(1).joinToString("-")
            if (projId != config.projectId) {
                println("error: incorrect prefix \"$projId\" for testcase id, only \"${config.projectId}\" is allowed")
                return null
            }
        }
        if (parsed == null) {
            println("error: unable to parse testcase id, allowed id format is [testcase number] or ${config.projectId}-[testcase number]")
            return null
        }
        return parsed
    }

    private fun getTestcaseFiles(nameFilter: ((String) -> Boolean)?): List<Pair<String, String>> {
        val all = fileProcessor.getAllTestcaseFiles()
        return nameFilter?.let { f -> all.filter { f(it.first) } } ?: all
    }

    private fun createFile(testCase: TestCase, replaceOld: Boolean) {
        println("generating testcase ${testCase.fullName} template")
        val fileNames = fileProcessor.createFile(testCase, replaceOld)
        println("${fileNames.first} was generated")
        fileNames.second?.also { println("old file was moved to $it") }
    }

    private fun downloadTestCase(id: Int, verbose: Boolean = true): TestCase? {
        if (verbose) { println("downloading testcase ${config.projectId}-$id") }
        return when (val res = testPalmInteractor.downloadTestCase(id)) {
            is TestPalmInteractor.DownloadResult.Success -> res.case
            is TestPalmInteractor.DownloadResult.Error -> {
                println("error: ${res.message}")
                null
            }
        }
    }

    private fun getTestCaseStatus(testCase: TestCase): TestCaseStatus =
        if (testCaseFileExists(testCase)) {
            if (testCaseIsUpToDate(testCase)) TestCaseStatus.UP_TO_DATE else TestCaseStatus.OUTDATED
        } else TestCaseStatus.REMOTE

    private fun testCaseFileExists(testCase: TestCase): Boolean = fileProcessor.fileExists(testCase)

    private fun testCaseIsUpToDate(testCase: TestCase): Boolean =
        fileProcessor.getHashFromFile(testCase) == testCase.hashCode()
}
