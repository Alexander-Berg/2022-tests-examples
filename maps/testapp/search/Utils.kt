package com.yandex.maps.testapp.search

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.preference.PreferenceManager
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Base64
import android.view.View
import com.google.gson.GsonBuilder
import com.yandex.mapkit.GeoObject
import com.yandex.mapkit.geometry.BoundingBox
import com.yandex.mapkit.geometry.Geometry
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polyline
import com.yandex.mapkit.search.*
import com.yandex.mapkit.uri.UriObjectMetadata
import com.yandex.maps.testapp.SearchBox
import com.yandex.maps.testapp.Utils
import com.yandex.runtime.ByteBufferUtils
import com.yandex.runtime.Error
import com.yandex.runtime.bindings.Serializable
import com.yandex.runtime.bindings.Serialization
import kotlin.math.*

private const val CAMERA_EMOJI = "\uD83D\uDCF7"

// string functions
fun toString(p: Point) = "(%.6f,%.6f)".format(p.latitude, p.longitude)
fun toString(b: BoundingBox) = "[${toString(b.southWest)} - ${toString(b.northEast)}]"

// geometry (point, bbox, etc.) functions
fun Point.shift(dlat: Double, dlon: Double) = Point(this.latitude + dlat, this.longitude + dlon)
fun BoundingBox.shift(dlat: Double, dlon: Double) =
    BoundingBox(this.southWest.shift(dlat, dlon), this.northEast.shift(dlat, dlon))

fun makeBoundingBox(p: Point, delta: Double) =
    BoundingBox(p.shift(-delta, -delta), p.shift(delta, delta))

fun BoundingBox.contains(p: Point): Boolean {
    val sw = this.southWest
    val ne = this.northEast
    return p.latitude in sw.latitude .. ne.latitude
        && p.longitude in sw.longitude .. ne.longitude
}

fun Point.asGeometry() = Geometry.fromPoint(this)
fun BoundingBox.asGeometry() = Geometry.fromBoundingBox(this)
fun Polyline.asGeometry() = Geometry.fromPolyline(this)

// route functions
fun makeRoute(points: Array<Point>, n: Int): Polyline {
    val result = java.util.ArrayList<Point>()
    points.indices
        .drop(1)
        .flatMap { line(points[it-1], points[it], n) }
        .toCollection(result)
    return Polyline(result)
}

private fun line(p: Point, q: Point, segments: Int): List<Point> {
    val dlat = (q.latitude - p.latitude) / segments
    val dlon = (q.longitude - p.longitude) / segments
    return (0..segments).map { Point(p.latitude + it * dlat, p.longitude + it * dlon) }
}

// distance functions
fun Point.distances(points: List<Point>) = points.map { distance(this, it) }
fun Polyline.distances(points: List<Point>) = points.map { distance(this, it) }

private fun sphericalDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dlat = lat2 - lat1
    val dlon = lon2 - lon1
    val a = sin(dlat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dlon / 2).pow(2)
    return 2 * atan2(sqrt(a), sqrt(1 - a))
}

private fun distance(p: Point, q: Point): Double {
    val EARTH_RADIUS = 6378137.0
    return EARTH_RADIUS * sphericalDistance(
        Math.toRadians(p.latitude), Math.toRadians(p.longitude),
        Math.toRadians(q.latitude), Math.toRadians(q.longitude))
}

private fun distance(route: Polyline, point: Point) =
    route.points.map { distance(point, it) }.minOrNull()!!

// misc functions

inline fun <reified T: Any> GeoObject.metadata(): T? = metadataContainer.getItem(T::class.java)
fun GeoObject.uri() = metadata<UriObjectMetadata>()?.uris?.firstOrNull()?.value

val Response.results: List<GeoObject> get() = this.collection.children.mapNotNull { it.obj }
val Response.names: List<String> get() = this.results.mapNotNull { it.name }
val Response.points: List<Point>
    get() = this.results.flatMap { it.geometry }.mapNotNull { it.point }
val Response.businessFilters: List<Filter>
    get() = this.metadata.businessResultMetadata!!.businessFilters.flatMap { it.toFilter() }

fun goodsItemWithDetails(goods: Goods) : ItemWithDetails {
    var text = goods.price?.text ?: ""
    goods.unit?.let { text += (if (text.isEmpty()) "" else " / ") + it }
    text += (if (text.isEmpty()) "" else " | ") + goods.name

    val description = listOf(
        CAMERA_EMOJI.repeat(goods.links.count()),
        goods.description ?: "",
        goods.tags.joinToString(",")
    ).filter { it.isNotEmpty() }.joinToString(" | ")

    return ItemWithDetails(text, description)
}

// SearchOptions

const val ALL_SNIPPETS = -1
const val ALL_SEARCH_TYPES = -1

fun makeSearchOptions(searchTypes: Int?, extendedSearchTypes: Int?): SearchOptions {
    val s = SearchOptions()
    if (searchTypes != null)
        s.searchTypes = searchTypes
    s.extendedSearchTypes = extendedSearchTypes
    s.advertPageId = "fake"
    return s
}

fun getSearchOptions(activity: Activity): SearchOptions {
    val preferences = PreferenceManager.getDefaultSharedPreferences(activity)
    val searchTypesMap = hashMapOf(
        "addresses"     to SearchType.GEO.value,
        "organizations" to SearchType.BIZ.value
    )

    val extendedSearchTypesMap = hashMapOf(
        "collections"   to ExtendedSearchType.COLLECTIONS.value,
        "transit"       to ExtendedSearchType.TRANSIT.value,
        "direct"        to ExtendedSearchType.DIRECT.value,
        "goods"         to ExtendedSearchType.GOODS.value,
        "poi"           to ExtendedSearchType.POINTS_OF_INTEREST.value,
        "masstransit"   to ExtendedSearchType.MASS_TRANSIT.value
    )

    val searchTypes = searchTypesMap
        .filter { preferences.getBoolean("search_sources_" + it.key, true) }
        .map { it.value }
        .fold(0) { x, y -> x or y }

    var extendedSearchTypes: Int? = extendedSearchTypesMap
        .filter { preferences.getBoolean("search_sources_" + it.key, true) }
        .map { it.value }
        .fold(0) { x, y -> x or y }

    if (extendedSearchTypes == 0)
        extendedSearchTypes = null

    var pageId = preferences.getString("search_advert_custom_page_id", null)
    if (pageId == null || pageId.isEmpty()) {
        pageId = preferences.getString("search_advert_page_id", "maps")
    }

    return makeSearchOptions(searchTypes, extendedSearchTypes)
        .setOrigin("mobile-maps-searchnearby-text")
        .setGeometry(true)
        .setAdvertPageId(pageId)
}

// UI helpers

inline fun <reified T : View> Activity.find(id: Int): T = findViewById(id)
inline fun <reified T : View> View.find(id: Int): T = findViewById(id)
fun View.hide() { visibility = View.GONE }
fun View.show() { visibility = View.VISIBLE }

fun showErrorMessage(context: Context, error: Error) {
    if (error is CacheUnavailableError) {
        Utils.showMessage(context, "No cache available for current position")
    } else {
        Utils.showError(context, error)
    }
}

open class BaseSearchBoxListener : SearchBox.SearchBoxListener {
    override fun onTextChanged(text: String) {}
    override fun onSubmit(text: String) {}
    override fun onOptionsClick() {}
}

// Context helpers

inline fun <reified T> Context.systemService(name: String) = getSystemService(name) as T

// Store/load serializable objects in settings, useful for debugging

@Suppress("unused")
inline fun <reified T: Serializable> loadObject(context: Context, key: String): T {
    return Serialization.deserialize(
        ByteBufferUtils.fromByteArray(
            Base64.decode(
                PreferenceManager
                    .getDefaultSharedPreferences(context)
                    .getString(key, ""),
                0
            )
        ),
        T::class.java
    )
}

@Suppress("unused")
fun <T: Serializable> storeObject(context: Context, key: String, item: T) {
    PreferenceManager
        .getDefaultSharedPreferences(context)
        .edit()
        .putString(key, Base64.encodeToString(
            Serialization.serializeToBytes(item), 0
        ))
        .apply()
}

fun <T> informOnEmptyResults(context: Context, results: List<T>) {
    if (results.isEmpty()) {
        Utils.showMessage(context, "Nothing was found")
    }
}

fun toJson(src: Any?) = GsonBuilder()
    .setPrettyPrinting()
    .disableHtmlEscaping()
    .create()
    .toJson(src)

fun highlightMatches(needle: String, haystack: String): SpannableString {
    val result = SpannableString(haystack)
    val regex = Regex(needle, setOf(
        RegexOption.LITERAL,
        RegexOption.IGNORE_CASE)
    )
    regex.findAll(haystack).forEach {
        result.setSpan(
            ForegroundColorSpan(Color.GREEN),
            it.range.first,
            it.range.last + 1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
    return result
}


