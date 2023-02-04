package com.yandex.maps.testapp.search.test

import com.yandex.mapkit.geometry.Geometry
import com.yandex.mapkit.geometry.Point
import com.yandex.maps.testapp.search.PerformanceTestCase
import com.yandex.maps.testapp.search.asGeometry
import com.yandex.maps.testapp.search.test.QueryGenerator.Companion.randomQuery

private fun randomDouble(min : Double, max : Double) : Double {
    return min + Math.random() * (max - min)
}

private fun randomMskPoint() : Geometry {
    return Point(
        randomDouble(55.33, 56.14),
        randomDouble(36.86, 38.45)
    ).asGeometry()
}

private fun randomSpbPoint() : Geometry {
    return Point(
        randomDouble(59.49, 60.39),
        randomDouble(28.78, 31.46)
    ).asGeometry()
}

// TODO(bkuchin): Add more tests:
// x Run multiple cache switching queries
// * Run multiple rubric queries (should we use "complex queries")
// * Run multiple 1-org queries, build histogram of response times?
// * Run multiple reverse biz search queries
// * Run multiple toponym queries
// * Run multiple reverse toponym queries
// * Run multiple what-where queries (are we able to get 100 queries here?)
// * Should we run all queries only on Moscow? Or in other regions too?
// * Fetch next page tests here?
fun generateTestCaseList(): List<PerformanceTestCase> {
    val result = mutableListOf<PerformanceTestCase>()
    fun moscowTestCase() = PerformanceTestCase(randomQuery(), randomMskPoint(), "Moscow", true)
    fun spbTestCase() = PerformanceTestCase(randomQuery(), randomSpbPoint(), "SPB", false)

    val mainTestCaseCount = 10
    val cacheCleanupTestCaseCount = 500
    for (i in 1..mainTestCaseCount) {
        for (j in 1..cacheCleanupTestCaseCount) {
            result.add(spbTestCase())
        }
        result.add(moscowTestCase())
    }
    return result
}

