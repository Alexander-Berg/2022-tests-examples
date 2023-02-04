package com.yandex.maps.testapp.search.test

import android.graphics.Color

class TestRunner(private val output: TestOutput) {
    private var tests = mutableListOf<TestCase>()
    var onFinished = {}

    fun addTest(test: TestCase) {
        tests.add(test)
    }

    fun start() {
        tests.forEachIndexed {n, test ->
            test.output = output
            test.onFinished = { runTest(n + 1) }
        }
        runTest(0)
    }

    private fun runTest(n: Int) {
        if (n >= tests.size) {
            reportResults()
            onFinished()
        } else {
            tests[n].run()
        }
    }

    private fun reportResults() {
        output.message("\n", Color.GREEN)
        reportCount(Result.Passed, Color.GREEN)
        reportCount(Result.Skipped, Color.YELLOW)
        reportCount(Result.Failed, Color.RED)
    }

    private fun reportCount(result: Result, color: Int) {
        val count = tests.filter{ it.result == result }.size
        if (count > 0)
            output.message("\t$count tests ${result.name.toLowerCase()}", color)
    }
}