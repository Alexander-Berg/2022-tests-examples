package com.yandex.maps.testapp.map

import android.os.Bundle
import android.view.View
import com.yandex.mapkit.RawTile
import com.yandex.mapkit.TileId
import com.yandex.mapkit.ZoomRange
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.geo.Projections
import com.yandex.mapkit.geometry.geo.XYPoint
import com.yandex.mapkit.images.DefaultImageUrlProvider
import com.yandex.mapkit.images.ImageUrlProvider
import com.yandex.mapkit.layers.Layer
import com.yandex.mapkit.layers.LayerLoadedListener
import com.yandex.mapkit.layers.LayerOptions
import com.yandex.mapkit.map.MapType
import com.yandex.mapkit.tiles.DefaultUrlProvider
import com.yandex.mapkit.tiles.TileProvider
import com.yandex.mapkit.tiles.UrlProvider
import com.yandex.maps.testapp.R
import java.io.InputStream
import java.nio.charset.Charset
import java.util.*
import java.util.logging.Logger

class GeoJSONActivity: MapBaseActivity() {
    private companion object {
        val LOGGER = Logger.getLogger("yandex.geojson")!!
        val rand = Random()
        const val zoom = 30
    }

    private var layer: Layer? = null
    private var tileProvider: TileProvider? = null
    private var imageProvider: ImageUrlProvider = ImageUrlProvider {desc -> "https://i.ibb.co/${desc.imageId}" }

    private fun create(tileGenerator: (TileId) -> String) {
        layer?.remove()

        tileProvider = TileProvider{tileId, version, etag ->
            RawTile(version, etag, RawTile.State.OK, tileGenerator(tileId).toByteArray()) }

        layer = mapview.map.addGeoJSONLayer(
            "geo_json_layer",
            style(),
            LayerOptions(),
            tileProvider ?: return,
            imageProvider,
            Projections.getWgs84Mercator(),
            ArrayList<ZoomRange>())
        layer?.invalidate("0")
        layer?.setLayerLoadedListener(layerLoadedListener)
    }

    private val layerLoadedListener = object : LayerLoadedListener {
        override fun onLayerLoaded() {
            LOGGER.warning("Layer loaded!")
        }
    }

    fun createLayer(view: View) {
        create { emptyTile() }
    }

    fun createPolylineTestLayer(view: View) {
        create { testPolyline(it) }
    }

    fun createPolylineLabelsTestLayer(view: View) {
        create { testPolylineLabels(it) }
    }

    fun createPolylineIconsTestLayer(view: View) {
        create { testPolylineIcons(it) }
    }

    fun createPolygonTestLayer(view: View) {
        create { testPolygon(it) }
    }

    fun createAnimatedPolygonTestLayer(view: View) {
        create { testAnimatedPolygon(it) }
    }

    fun createPointTestLayer(view: View) {
        create { testPoint(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.geo_json)
        super.onCreate(savedInstanceState)
        super.setMapType(MapType.NONE)
    }

    private infix fun XYPoint.vecMul(that: XYPoint): Double {
        return x * that.y - y * that.x
    }

    private infix operator fun XYPoint.minus(that: XYPoint): XYPoint {
        return XYPoint(x - that.x, y - that.y)
    }

    private fun XYPoint.toWorld(): Point {
        return Projections.getWgs84Mercator().xyToWorld(this, zoom)
    }

    private fun Point.toXY(): XYPoint {
        return Projections.getWgs84Mercator().worldToXY(this, zoom)
    }

    // Can't import kotlin.math
    private inline val Double.sign: Int
        inline get() = if (this < 0) -1 else if (this > 0) 1 else 0

    private fun intersect(start1: XYPoint, dir1: XYPoint, start2: XYPoint, dir2: XYPoint): XYPoint {
        assert((dir2 vecMul dir1) != .0) { "Directions mustn't be collinear" }
        return XYPoint(
            ((start1 vecMul dir1) * dir2.x - (start2 vecMul dir2) * dir1.x) / (dir2 vecMul dir1),
            ((start1 vecMul dir1) * dir2.y - (start2 vecMul dir2) * dir1.y) / (dir2 vecMul dir1)
        )
    }

    private fun doesIntersect(a1: XYPoint, b1: XYPoint, a2: XYPoint, b2: XYPoint): Boolean {
        return ((b2 - a1) vecMul (b1 - a1)).sign != ((a2 - a1) vecMul (b1 - a1)).sign &&
               ((b1 - a2) vecMul (b2 - a2)).sign != ((a1 - a2) vecMul (b2 - a2)).sign
    }

    private fun pointFromTile(tileId: TileId, normX: Double, normY: Double): Point {
        val tileSize = (1 shl (zoom - tileId.z))
        val fromX = tileId.x * tileSize
        val fromY = tileId.y * tileSize
        val x = normX * tileSize + fromX
        val y = normY * tileSize + fromY
        return XYPoint(x, y).toWorld()
    }

    private fun randomPointFromTile(tileId: TileId): Point {
        return pointFromTile(tileId, rand.nextDouble(), rand.nextDouble())
    }

    private fun style() =
        """
        {
            "layers": [
            {
                "id": "Background",
                "minzoom": 0,
                "maxzoom": 21,
                "type": "background",
                "style": {
                    "background-color": "#888888",
                    "background-opacity": 0.3
                }
            },
            {
                "id": "RedPolygon",
                "minzoom": 0,
                "maxzoom": 21,
                "type": "fill",
                "source-layer": "red-polygon",
                "style": {
                    "fill-color": "#ff0000",
                    "fill-opacity": 1
                }
            },
            {
                "id": "BlueLine",
                "minzoom": 0,
                "maxzoom": 21,
                "type": "line",
                "source-layer": "blue-line",
                "style": {
                    "line-color": "#0000ff",
                    "line-opacity": 1,
                    "line-width": 10,
                    "line-border-width": 2,
                    "line-border-color": "#000000",
                    "line-border-opacity": 1
                }
            },
            {
                "id": "Point",
                "type": "label",
                "minzoom": 0,
                "maxzoom": 21,
                "source-layer": "point",
                "style": {
                    "label-target": "point",
                    "icon-scale": 4,
                    "icon-image": "9tNbGX5/a0.png",
                    "text-field": "text",
                    "text-size": 12,
                    "text-font": "YSText-Regular.ttf",
                    "alt-text-field": "alt-text",
                    "alt-text-size": 8,
                    "text-offset": 0,
                    "text-anchor": ["right", "bottom"],
                    "icon-offset": [0, -0.5],
                    "text-offset-from-icon": true,
                    "text-anchor-icon-pos": [0, 0.25]
                }
            },
            {
                "id": "AnimatedPolygon",
                "type": "fill",
                "minzoom": 0,
                "maxzoom": 21,
                "source-layer": "animated-polygon",
                "style": {
                    "fill-pattern-scale": 2,
                    "fill-pattern": "GCThJhV/snow-falling.png"
                }
            },
            {
                "id": "WhiteLine",
                "minzoom": 0,
                "maxzoom": 21,
                "type": "line",
                "source-layer": "labeled-line",
                "style": {
                    "line-color": "#ffffff",
                    "line-opacity": 1,
                    "line-width": 12,
                    "line-border-width": 2,
                    "line-border-color": "#000000",
                    "line-border-opacity": 1
                }
            },
            {
                "id": "PolylineLabel",
                "type": "label",
                "minzoom": 0,
                "maxzoom": 21,
                "source-layer": "labeled-line",
                "style": {
                    "label-target": "line",
                    "text-field": "text",
                    "text-size": 10,
                    "text-font": "YSText-Regular.ttf",
                    "alt-text-field": "alt-text",
                    "alt-text-size": 8,
                    "label-repeat-distance": 20
                }
            },
            {
                "id": "ConflictResolver",
                "type": "label",
                "minzoom": 0,
                "maxzoom": 21,
                "source-layer": "conflictResolver",
                "style": {
                    "label-target": "point",
                    "icon-scale": 4,
                    "icon-image": "9tNbGX5/a0.png",
                    "text-field": "text",
                    "text-size": 12,
                    "text-font": "YSText-Regular.ttf",
                    "alt-text-field": "alt-text",
                    "alt-text-size": 8,
                    "text-offset": 0,
                    "text-anchor": ["right", "bottom"],
                    "icon-offset": [0, -0.5],
                    "text-offset-from-icon": true,
                    "text-anchor-icon-pos": [0, 0.25],
                    "label-priority": ["get", "priority"],
                    "icon-ignore-conflicts": ["get", "ignore-conflicts"]
                }
            },
            {
                "id": "BlueLineForIcons",
                "minzoom": 0,
                "maxzoom": 21,
                "type": "line",
                "source-layer": "icon-line",
                "style": {
                    "line-color": "#0000ff",
                    "line-opacity": 1,
                    "line-width": 10,
                    "line-border-width": 2,
                    "line-border-color": "#000000",
                    "line-border-opacity": 1
                }
            },
            {
                "id": "PolylineIcon",
                "type": "label",
                "minzoom": 0,
                "maxzoom": 21,
                "source-layer": "icon-line",
                "style": {
                    "label-target": "line",
                    "icon-scale": 4,
                    "icon-image": "9tNbGX5/a0.png",
                    "label-repeat-distance": 50
                }
            }]
        }
        """.trimIndent()

    private fun testPolyline(tileId: TileId): String {
        val points = Array(3) { randomPointFromTile(tileId) }

        return """
            {
                "layers": [
                    {
                        "type": "FeatureCollection",
                        "name": "blue-line",
                        "features": [
                            {
                                "type": "Feature",
                                "geometry":
                                    {
                                        "type": "LineString",
                                        "coordinates": [
                                            ${
                                                points.joinToString(", ") { "[${it.longitude}, ${it.latitude}]" }
                                            }
                                        ]
                                    },
                                "properties": null,
                                "id": "Polyline_${tileId.x}_${tileId.y}_${tileId.z}"
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()
    }

    private fun testPolylineLabels(tileId: TileId): String {
        val points = Array(3) { randomPointFromTile(tileId) }

        return """
            {
                "layers": [
                    {
                        "type": "FeatureCollection",
                        "name": "labeled-line",
                        "features": [
                            {
                                "type": "Feature",
                                "geometry":
                                    {
                                        "type": "LineString",
                                        "coordinates": [
                                            ${
                                                points.joinToString(", ") { "[${it.longitude}, ${it.latitude}]" }
                                            }
                                        ]
                                    },
                                "properties":
                                    {
                                        "text": "Text",
                                        "alt-text": "Текст"
                                    },
                                "id": "Polyline_${tileId.x}_${tileId.y}_${tileId.z}"
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()
    }

    private fun testPolylineIcons(tileId: TileId): String {
        val points = Array(3) { randomPointFromTile(tileId) }

        return """
            {
                "layers": [
                    {
                        "type": "FeatureCollection",
                        "name": "icon-line",
                        "features": [
                            {
                                "type": "Feature",
                                "geometry":
                                    {
                                        "type": "LineString",
                                        "coordinates": [
                                            ${
                                                points.joinToString(", ") { "[${it.longitude}, ${it.latitude}]" }
                                            }
                                        ]
                                    },
                                "properties":
                                    {
                                    },
                                "id": "Polyline_${tileId.x}_${tileId.y}_${tileId.z}"
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()
    }

    private fun generatePolygonPoints(tileId: TileId): MutableList<Point> {
        val points = Array(3) { randomPointFromTile(tileId) }.toMutableList()
        points.add(points[0]) // By geoJSON specification first and last points of lineRing must be the same
        if ((points[1].toXY() - points[0].toXY()) vecMul (points[2].toXY() - points[1].toXY()) < 0) {
            points.reverse() // first ring must be counterclockwise
        }
        return points
    }

    private fun testPolygon(tileId: TileId): String {

        val points = generatePolygonPoints(tileId)

        return """
            {
                "layers": [
                    {
                        "type": "FeatureCollection",
                        "name": "red-polygon",
                        "features": [
                            {
                                "type": "Feature",
                                "geometry":
                                    {
                                        "type": "Polygon",
                                        "coordinates": [[
                                            ${
                                                points.joinToString(", ") { "[${it.longitude}, ${it.latitude}]" }
                                            }
                                        ]]
                                    },
                                "properties": null,
                                "id": "Polygon_${tileId.x}_${tileId.y}_${tileId.z}"
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()
    }

    private fun testAnimatedPolygon(tileId: TileId): String {
        val points = generatePolygonPoints(tileId)
        return """
            {
                "layers" : [
                    {
                        "type": "FeatureCollection",
                        "name": "animated-polygon",
                        "features": [
                            {
                                "type": "Feature",
                                "geometry":
                                    {
                                        "type": "Polygon",
                                        "coordinates": [[
                                            ${
                                                points.joinToString(", ") { "[${it.longitude}, ${it.latitude}]" }
                                            }
                                        ]]
                                    },
                                "properties": null,
                                "id": "Polygon_${tileId.x}_${tileId.y}_${tileId.z}"
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()
    }

    private fun testPoint(tileId: TileId): String {
        val point = randomPointFromTile(tileId)

        return """
            {
                "layers": [
                    {
                        "type": "FeatureCollection",
                        "name": "point",
                        "features": [
                            {
                                "type": "Feature",
                                "geometry":
                                    {
                                        "type": "Point",
                                        "coordinates": [${point.longitude}, ${point.latitude}]
                                    },
                                "properties":
                                    {
                                        "text": "Tile ID: ${tileId.x},${tileId.y},${tileId.z}",
                                        "alt-text": "альтернативный текст"
                                    },
                                "id": "Point_${tileId.x}_${tileId.y}_${tileId.z}"
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()
    }

    private fun testConflictsResolver(tileId: TileId): String {
        val point1 = pointFromTile(tileId, 0.6, 0.6)
        val point2 = pointFromTile(tileId, 0.5, 0.5)
        val point3 = pointFromTile(tileId, 0.4, 0.4)

        return """
            {
                "layers": [
                    {
                        "type": "FeatureCollection",
                        "name": "conflictResolver",
                        "features": [
                            {
                                "type": "Feature",
                                "geometry":
                                    {
                                        "type": "Point",
                                        "coordinates": [${point1.longitude}, ${point1.latitude}]
                                    },
                                "properties":
                                    {
                                        "text": "1",
                                        "priority": 100,
                                        "ignore-conflicts": false
                                    },
                                "id": "Point_${tileId.x}_${tileId.y}_${tileId.z}_1"
                            },
                            {
                                "type": "Feature",
                                "geometry":
                                    {
                                        "type": "Point",
                                         "coordinates": [${point2.longitude}, ${point2.latitude}]
                                    },
                                "properties":
                                    {
                                        "text": "2",
                                        "priority": 99,
                                        "ignore-conflicts": true
                                    },
                                "id": "Point_${tileId.x}_${tileId.y}_${tileId.z}_2"
                            },
                            {
                                "type": "Feature",
                                "geometry":
                                    {
                                        "type": "Point",
                                        "coordinates": [${point3.longitude}, ${point3.latitude}]
                                    },
                                "properties":
                                    {
                                        "text": "3",
                                        "priority": 98,
                                        "ignore-conflicts": false
                                    },
                                "id": "Point_${tileId.x}_${tileId.y}_${tileId.z}_3"
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()
    }

    private fun emptyTile() =
        """
            {
                "layers": [
                    {
                        "type": "FeatureCollection",
                        "name": "null",
                        "features": []
                    }
                ]
            }
        """.trimIndent()
}
