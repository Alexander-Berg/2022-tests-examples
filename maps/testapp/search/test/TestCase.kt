package com.yandex.maps.testapp.search.test

import android.graphics.Color

interface TestOutput {
    fun message(text: String, color: Int)
}

enum class Result { Passed, Skipped, Failed }

open class TestCase(val name: String, private val skip: Boolean) {
    var result = if (skip) Result.Skipped else Result.Passed

    var output: TestOutput? = null
    var onFinished = {}
    var finished = false

    open fun doTest() {}

    fun run() {
        print(name)
        if (skip) {
            finish()
        } else {
            doTest()
        }
    }

    fun finish() {
        if (finished) {
            print("test $name finished more than once", Color.RED)
            return
        }
        finished = true

        when (result) {
            Result.Failed -> print("Fail", Color.RED)
            Result.Skipped -> print("Skipped", Color.YELLOW)
            Result.Passed -> print("Done", Color.GREEN)
        }
        onFinished()
    }

    fun <T> checkThat(value: T?, predicate: Predicate<T>, msg: String) {
        if (!predicate.invoke(value)) {
            fail("Failed: $msg [${predicate.errorMessage()}]")
        }
    }

    fun fail(msg: String) {
        print(msg, Color.RED)
        result = Result.Failed
    }

    private fun print(text: String, color: Int = Color.GRAY) {
        output?.message(text, color)
    }
}
