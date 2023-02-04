package com.yandex.maps.testapp.map

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.annotation.UiThread
import androidx.core.util.Consumer

import com.yandex.mapkit.RawTile
import com.yandex.mapkit.TileId
import com.yandex.mapkit.ZoomRange
import com.yandex.mapkit.geometry.geo.Projections
import com.yandex.mapkit.geometry.geo.XYPoint
import com.yandex.mapkit.images.DefaultImageUrlProvider
import com.yandex.mapkit.layers.Layer
import com.yandex.mapkit.layers.LayerOptions
import com.yandex.mapkit.layers.OverzoomMode
import com.yandex.mapkit.tiles.TileProvider

import com.yandex.mapkit.Animation
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.layers.LayerLoadedListener
import com.yandex.mapkit.logo.Alignment
import com.yandex.mapkit.logo.HorizontalAlignment
import com.yandex.mapkit.logo.VerticalAlignment
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.DataProvider
import com.yandex.mapkit.map.HeatmapLayer
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.map.MapType
import com.yandex.maps.testapp.R
import com.yandex.maps.testapp.Utils
import com.yandex.maps.testapp.VulkanTools

import java.lang.ref.WeakReference
import java.util.*
import java.util.logging.Logger

class HeatmapActivity: MapBaseActivity() {
    private companion object {
        val LOGGER = Logger.getLogger("yandex.heatmap")
        const val zoom = 30
    }

    private class ComparableTileId(val tileId: TileId) {
        override fun equals(other: Any?): Boolean = (other is ComparableTileId) && tileId.x == other.tileId.x && tileId.y == other.tileId.y && tileId.z == other.tileId.z
        override fun hashCode(): Int = Triple(tileId.x,tileId.y,tileId.z).hashCode()
    }

    private data class WeightedPoint(val latitude: String, val longitude: String, val weight: String) {
        val xyPoint: XYPoint = Projections.getWgs84Mercator().worldToXY(Point(latitude.toDouble(), longitude.toDouble()), zoom)
    }

    private enum class StyleTarget {
        SURGE,
        EARTHQUAKES,
        CUSTOM,
        TILED,
    }

    private var vulkanWasPreferred: Boolean = false
    private var tiledLayer: Layer? = null
    private var layer: HeatmapLayer? = null
    private val layerId: String = "heatmap_layer"
    private val tiledLayerId: String = "tiled_heatmap_layer"
    private val demo = Demo()
    private val customLayer = CustomLayer()
    private var pointsNum: Int = 50
    private var activeLayer = StyleTarget.EARTHQUAKES
    private var surgePoints: String? = null
    private lateinit var styleEditor: StyleEditor
    private lateinit var runDemoButton: Button
    private lateinit var runTestButton: Button
    private lateinit var dataLoaderButton: Button
    private lateinit var showLayerButton: RadioButton
    private lateinit var changeLayerTopLineButton: RadioGroup
    private lateinit var changeLayerBottomLineButton: RadioGroup
    private var tileProvider: TileProvider? = null

    private val defaultLayerLoadedListener = LayerLoadedListener {
        showLayerButton.performClick()
        LOGGER.warning("Heatmap layer is loaded!")
    }

    @UiThread
    private fun setNightMode(makeNight: Boolean) {
        val nightModeButton: CompoundButton = findViewById(R.id.night_mode_switch)
        if (nightModeButton.isChecked != makeNight)
            nightModeButton.performClick()
    }

    private fun goToAlaska() {
        mapview.map.move(CameraPosition(Point(63.1016, -151.5129), 7.0f, 356.406f, 0.0f))
    }

    private fun goToMoscow() {
        mapview.map.move(CameraPosition(Point(55.755793, 37.617134), 9.0f, 0.0f, 0.0f))
    }

    @UiThread
    fun onChangeActiveClicked(view: View) {
        layer?.activate(view.id == R.id.active_on)
        tiledLayer?.activate(view.id == R.id.active_on)
    }

    @UiThread
    private fun clearSelectionOfOtherLine(view: View) {
        when (view.id) {
            R.id.surge_layer       -> changeLayerBottomLineButton.clearCheck()
            R.id.earthquakes_layer -> changeLayerBottomLineButton.clearCheck()
            R.id.custom_layer      -> changeLayerTopLineButton.clearCheck()
            R.id.tiled_layer       -> changeLayerTopLineButton.clearCheck()
        }
    }

    @UiThread
    fun onChangeLayerClicked(view: View) {
        clearSelectionOfOtherLine(view)
        when (view.id) {
            R.id.surge_layer       -> activeLayer = StyleTarget.SURGE
            R.id.earthquakes_layer -> activeLayer = StyleTarget.EARTHQUAKES
            R.id.custom_layer      -> activeLayer = StyleTarget.CUSTOM
            R.id.tiled_layer       -> activeLayer = StyleTarget.TILED
        }
        createLayer()
    }

    @UiThread
    fun openStyleEditor(view: View) {
        styleEditor.show()
    }

    @UiThread
    private fun createLayer() {
        runTestButton.visibility = View.VISIBLE
        runDemoButton.visibility = View.GONE
        dataLoaderButton.visibility = View.GONE
        layer?.remove()
        tiledLayer?.remove()
        layer = null
        tiledLayer = null

        when (activeLayer) {
            StyleTarget.SURGE -> createSurgeLayer()
            StyleTarget.EARTHQUAKES -> createEarthquakesLayer()
            StyleTarget.CUSTOM -> createCustomLayer()
            StyleTarget.TILED -> createTiledLayer()
        }
    }

    @UiThread
    private fun createSurgeLayer() {
        setNightMode(false)
        val dataProvider = DataProvider{
            if (surgePoints == null)
                surgePoints = buildFeatureCollection(makeWeightedPoints(Utils.readResourceAsString(this, R.raw.raw_surge_points)))
            surgePoints!!
        }
        layer = mapview.map.addHeatmapLayer(layerId, styleProvider(StyleTarget.SURGE), dataProvider)
        layer!!.setLayerLoadedListener(defaultLayerLoadedListener)
        goToMoscow()
    }

    @UiThread
    private fun createEarthquakesLayer() {
        runDemoButton.visibility = View.VISIBLE
        setNightMode(true)
        val dataProvider = DataProvider{Utils.readResourceAsString(this, R.raw.earthquakes)}
        layer = mapview.map.addHeatmapLayer(layerId, styleProvider(StyleTarget.EARTHQUAKES), dataProvider)
        layer!!.setLayerLoadedListener(defaultLayerLoadedListener)
        goToAlaska()
    }

    @UiThread
    private fun createHeavyLayer(size : Int) {
        setNightMode(true)
        layer = mapview.map.addHeatmapLayer(layerId, styleProvider(StyleTarget.EARTHQUAKES), DataProvider{generateLargeGeoJSON(size)})
        layer!!.setLayerLoadedListener(defaultLayerLoadedListener)
    }

    @UiThread
    private fun createCustomLayer() {
        runTestButton.visibility = View.GONE
        dataLoaderButton.visibility = View.VISIBLE
        if (customLayer.points == null)
            return
        val dataProvider = DataProvider{ customLayer.points!! }
        layer = mapview.map.addHeatmapLayer(layerId, styleProvider(StyleTarget.CUSTOM), dataProvider)
        layer!!.setLayerLoadedListener(defaultLayerLoadedListener)
    }

    @UiThread
    private fun createTiledLayer() {
        setNightMode(false)
        if (tileProvider == null)
            tileProvider = makeTileProvider()
        tiledLayer = mapview.map.addGeoJSONLayer(
            tiledLayerId,
            style(StyleTarget.TILED),
            LayerOptions().setOverzoomMode(OverzoomMode.ENABLED).setTransparent(false),
            tileProvider!!,
            DefaultImageUrlProvider(),
            Projections.getWgs84Mercator(),
            ArrayList<ZoomRange>())
        tiledLayer!!.invalidate("0")
        tiledLayer!!.setLayerLoadedListener(defaultLayerLoadedListener)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        vulkanWasPreferred = VulkanTools.readVulkanPreferred(applicationContext)
        VulkanTools.storeVulkanPreferred(applicationContext, false)

        setContentView(R.layout.heatmap)
        vulkanSwitchBt.isEnabled = false

        super.onCreate(savedInstanceState)
        super.setMapType(MapType.VECTOR_MAP)
        mapview.map.logo.setAlignment(Alignment(HorizontalAlignment.RIGHT, VerticalAlignment.TOP))

        val hls : LinearLayout = findViewById(R.id.heavy_layer_settings)
        hls.visibility = View.GONE

        changeLayerTopLineButton = findViewById(R.id.change_layer_top_line)
        changeLayerBottomLineButton = findViewById(R.id.change_layer_bottom_line)
        showLayerButton = findViewById(R.id.active_on)
        runDemoButton = findViewById(R.id.run_demo)
        runTestButton = findViewById(R.id.run_test)
        dataLoaderButton = findViewById(R.id.data_loader)
        styleEditor = StyleEditor()
        createLayer()
    }

    override fun onDestroy() {
        VulkanTools.storeVulkanPreferred(applicationContext, vulkanWasPreferred)
        super.onDestroy()
    }

    fun pointsNumListenerInc(view: View) {
        pointsNum += 50;
        createHeavyLayer(pointsNum);
    }

    fun pointsNumListenerDec(view: View) {
        if (pointsNum > 50) {
            pointsNum -= 50;
            createHeavyLayer(pointsNum);
        }
    }

    @UiThread
    fun runDemo(view: View) {
        demo.run(demo.earthquakesDemoSteps)
    }

    @UiThread
    fun runTest(view: View) {
        when (activeLayer) {
            StyleTarget.EARTHQUAKES -> demo.run(demo.earthquakesTestSteps)
            StyleTarget.SURGE -> demo.run(demo.surgeTestSteps)
            StyleTarget.TILED -> demo.run(demo.surgeTestSteps)
            else -> { }
        }
    }

    fun showLoadDialog(view: View) {
        customLayer.showLoadDialog()
    }

    private class CameraCallbackImpl(var weakSelf: WeakReference<HeatmapActivity.Demo>): Map.CameraCallback {
        override fun onMoveFinished(completed: Boolean) {
            if (completed)
                weakSelf.get()?.nextStep()
        }
    }

    private inner class Demo {
        private var stepIndex: Int = 0
        private var demoLayerLoadedListener: LayerLoadedListener? = null
        private val durationFactor = 1f
        private var steps = arrayOf<PerformanceTestStep>()

        val earthquakesDemoSteps = arrayOf(
            PerformanceTestStep(CameraPosition(Point(15.332697, -94.4164474), 15.0f, 285.6555f, 0.0f), 0.0f),
            PerformanceTestStep(CameraPosition(Point(0.000148008555291, -89.09761217675691), 1.15f, 279.46274f, 0.0f), 10f * durationFactor),
            PerformanceTestStep(CameraPosition(Point(59.975890, -153.167710), 10.0f, 230.47562f, 0.0f), 10f * durationFactor)
        )

        val earthquakesTestSteps = arrayOf(
            PerformanceTestStep(CameraPosition(Point(0.000148008555291, -89.09761217675691), 1.15f, 279.46274f, 0.0f), 0.0f),
            PerformanceTestStep(CameraPosition(Point(59.975890, -153.167710), 10.0f, 230.47562f, 0.0f), 7f * durationFactor)
        )

        val surgeTestSteps = arrayOf(
            PerformanceTestStep(CameraPosition(Point(55.755793, 37.617134), 8.5f, 0.0f, 0.0f), 0.0f),
            PerformanceTestStep(CameraPosition(Point(55.755793, 37.617134), 8.5f, 0.0f, 70.0f), 2f * durationFactor),
            PerformanceTestStep(CameraPosition(Point(55.95, 37.95), 11.0f, 0.0f, 70.0f), 3f * durationFactor)
        )

        fun nextStep() {
            stepIndex++;
            if (stepIndex < steps.size) {
                val step = steps[stepIndex]
                mapview.map.move(step.position, Animation(Animation.Type.SMOOTH, step.time),
                    CameraCallbackImpl(WeakReference(this)))
            }
        }

        fun run(steps: Array<PerformanceTestStep>) {
            this.steps = steps
            stepIndex = 0;

            demoLayerLoadedListener = LayerLoadedListener {
                showLayerButton.performClick()
                nextStep()
            }

            mapview.map.move(steps[stepIndex].position)
            layer?.setLayerLoadedListener(demoLayerLoadedListener)
            tiledLayer?.setLayerLoadedListener(demoLayerLoadedListener)
        }
    }

    /// generate geoGSON with size*size points
    private fun generateLargeGeoJSON(size : Int) = buildString {
        val minX = 20f
        val maxX = 120f + minX
        val minY = -60f
        val maxY = 120f + minY
        val dx = (maxX - minX) / (size - 1)
        val dy = (maxY - minY) / (size - 1)
        append("""{"type":"FeatureCollection","features":[""")
        for (x in 0 until size) {
            for (y in 0 until size) {
                val lon = x * dx + minX
                val lat = y * dy + minY
                append("""{"type":"Feature","properties":{"mag":3},"geometry":{"type":"Point","coordinates":[$lon,$lat]}},""")
            }
        }
        // remove last ',' before ']'
        deleteCharAt(length - 1)
        append("]}")
    }

    private fun buildTile(points: ArrayList<WeightedPoint>) = buildString {
        append("""{"layers":[""")
        append(buildFeatureCollection(points))
        append("]}")
    }

    private fun buildFeatureCollection(points: ArrayList<WeightedPoint>) = buildString {
        append("""{"type":"FeatureCollection","name":"${tiledLayerId}","features":[""")

        for (point in points) {
            append("""{"type":"Feature","properties":{"surge":""")
            append(point.weight)
            append("""},"geometry":{"type":"Point","coordinates":[""")
            append(point.longitude)
            append(",")
            append(point.latitude)
            append("]}},")
        }

        // remove last ',' before ']'
        if (points.isNotEmpty())
            deleteCharAt(length - 1)

        append("""]}""")
    }

    private fun makeWeightedPoints(data: String): ArrayList<WeightedPoint> {
        val st = StringTokenizer(data, " \n")
        val points = ArrayList<WeightedPoint>()
        while (st.hasMoreTokens()) {
            val lon = st.nextToken()
            val lat = st.nextToken()
            val weight = st.nextToken()
            points.add(WeightedPoint(lat, lon, weight))
        }
        return points
    }

    private fun makeTileProvider(): TileProvider {
        val splitter = DataSplitter(makeWeightedPoints(Utils.readResourceAsString(this, R.raw.raw_surge_points)))
        val provider = Cache<ComparableTileId, ByteArray>{tileId -> buildTile(splitter.subset(tileId.tileId)).toByteArray()}
        return TileProvider{tileId, version, etag ->
            RawTile(version, etag, RawTile.State.OK, provider.get(ComparableTileId(tileId))) }
    }

    private fun styleProvider(target: StyleTarget) = DataProvider{ style(target) }

    private fun style(target: StyleTarget) : String {
        var sourceLayer = layerId
        if (target == StyleTarget.TILED)
            sourceLayer = tiledLayerId

        return """
        {
        "layers": [
            {
                "id": "Heatmap",
                "minzoom": 0,
                "maxzoom": 21,
                "type": "heatmap",
                "source-layer": "${sourceLayer}",
                "style": { ${styleEditor.styles[target]} }
            }]
        }
        """.trimIndent()
    }

    private inner class StyleEditor {

        val styles = mutableMapOf(
                StyleTarget.SURGE to Utils.readResourceAsString(this@HeatmapActivity, R.raw.surge_heatmap_style),
                StyleTarget.EARTHQUAKES to Utils.readResourceAsString(this@HeatmapActivity, R.raw.earthquakes_heatmap_style),
                StyleTarget.CUSTOM to "",
                StyleTarget.TILED to Utils.readResourceAsString(this@HeatmapActivity, R.raw.surge_heatmap_style)
                )

        private val styleHandler = object : MapCustomizationDialog.StyleHandler<StyleTarget> {
            override fun applyStyle(styleTarget: StyleTarget, style: String) {
                styles.put(styleTarget, style)
                activeLayer = styleTarget
                createLayer()
            }

            override fun saveStyle(styleTarget: StyleTarget, style: String) {
                applyStyle(styleTarget, style)
            }
        }

        private var styleEditor = MapCustomizationDialog<StyleTarget>(
                this@HeatmapActivity,
                styleHandler,
                mapOf(StyleTarget.CUSTOM to R.menu.heatmap_customization_templates))

        fun show() = styleEditor.show(activeLayer, false)

        init {
            for (entry in styles.entries)
                styleEditor.setStyleText(entry.key, entry.value)
        }
    }

    private inner class CustomLayer {
        private var lastUsedUrl = ""

        var points: String? = null

        private val saveLastUsedUrl = Consumer<String> { url -> lastUsedUrl = url }

        private val onLoadingSuccess = Consumer<String> { json ->
            points = json
            createLayer()
        }

        fun showLoadDialog() = JsonLoaderDialog.show(
            lastUsedUrl,
            this@HeatmapActivity,
            onLoadingSuccess,
            saveLastUsedUrl)
    }

    private inner class DataSplitter(weightedPoints: ArrayList<WeightedPoint>) {
        private val points: List<WeightedPoint> = weightedPoints.sortedWith(compareBy {it.xyPoint.x})

        private fun lowerBound(bound: Double): Int {
            var l = -1
            var r = points.size
            while (l + 1 < r) {
                val mid = (l + r) / 2
                if (points[mid].xyPoint.x < bound)
                    l = mid
                else
                    r = mid
            }
            return r
        }

        fun subset(tileId: TileId): ArrayList<WeightedPoint> {
            val tileSize = (1 shl (zoom - tileId.z))
            val fromX = tileId.x * tileSize
            val fromY = tileId.y * tileSize
            val result = ArrayList<WeightedPoint>()
            val from = lowerBound(fromX.toDouble())
            val to = lowerBound((fromX + tileSize).toDouble())
            for (i in from until to) {
                val y = points[i].xyPoint.y
                if (fromY <= y && y < fromY + tileSize)
                    result.add(points[i])
            }
            return result
        }
    }

    private class Cache<K, V>(val generator: (K) -> V) {
        private val cache = mutableMapOf<K, V>()

        fun get(key: K): V {
            val cachedValue = cache[key]
            if (cachedValue != null)
                return cachedValue

            val newValue = generator(key)
            cache[key] = newValue
            return newValue
        }
    }
}
