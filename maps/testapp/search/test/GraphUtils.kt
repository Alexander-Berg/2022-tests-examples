package com.yandex.maps.testapp.search.test

import android.graphics.DashPathEffect
import android.graphics.Paint
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import com.yandex.maps.testapp.search.PerformanceTestCase

private fun <T1: Number, T2: Number> dataPoint(x: T1, y: T2) = DataPoint(x.toDouble(), y.toDouble())

private fun makeDashPaint(color: Int): Paint {
    val paint = Paint()
    paint.color = color
    paint.style = Paint.Style.FILL_AND_STROKE
    paint.strokeWidth = 3f
    paint.pathEffect = DashPathEffect(floatArrayOf(10f, 5f), 0f)
    return paint
}

fun makeAverageSeries(
    testCases: List<PerformanceTestCase>,
    color: Int
): LineGraphSeries<DataPoint> {
    val averageDuration = testCases.map { it.duration() }.average()
    return makeConstantValueSeries(averageDuration, 1, testCases.size, color)
}

fun makeConstantValueSeries(
    value: Double,
    minXValue: Int,
    maxXValue: Int,
    color: Int
): LineGraphSeries<DataPoint> {
    val result = LineGraphSeries<DataPoint>(arrayOf(
        dataPoint(minXValue, value),
        dataPoint(maxXValue, value)
    ))
    result.isDrawAsPath = true
    result.setCustomPaint(makeDashPaint(color))
    return result
}

fun makeTestResultSeries(
    testCases: List<PerformanceTestCase>,
    color: Int
): LineGraphSeries<DataPoint> {
    val result = LineGraphSeries<DataPoint>(testCases
        .mapIndexed { i, testCase -> dataPoint(i + 1, testCase.duration()) }
        .toTypedArray()
    )
    result.isDrawDataPoints = true
    result.color = color
    return result
}

