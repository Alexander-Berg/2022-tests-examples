package com.yandex.maps.testapp.common.internal.point_context

import com.yandex.mapkit.geometry.Point

import android.util.Base64

// WARNING! Nothing from here should be used in the end user applications!
//
// It is for Mapkit internal testing purposes only.
//
// Don't encode point context in end user applications. Its format is
// a subject to change. Here we do encoding to test routes with
// arrival and driving arrival points

class DrivingArrivalPoint(val point: Point, val id: String)

fun encode(s: String) : String {
    val data = s.toByteArray(Charsets.UTF_8)
    val base64str = Base64.encodeToString(data, Base64.URL_SAFE or Base64.NO_WRAP)
    return base64str.replace("=", ",")
}

fun encode(arrivalPoints: List<Point>,
           drivingArrivalPoints: List<DrivingArrivalPoint>) : String? {
    if (arrivalPoints.isEmpty() && drivingArrivalPoints.isEmpty()) {
        return null
    }

    val aps = arrivalPoints.map{"${it.longitude},${it.latitude}"}.joinToString(";")
    val daps = drivingArrivalPoints.map{
        "${it.point.longitude},${it.point.latitude},${it.id}"}.joinToString(";")
    return encode("v1|$aps|$daps")
}

fun encode(arrivalPoints: List<Point>) : String? {
    return encode(arrivalPoints, listOf())
}

fun decodeBase64(pointContext: String): String? {
    try {
        val data = Base64.decode(pointContext.replace(",", "="), Base64.URL_SAFE)
        return String(data, Charsets.UTF_8)
    } catch (e: IllegalArgumentException) {
        return null
    }
}
