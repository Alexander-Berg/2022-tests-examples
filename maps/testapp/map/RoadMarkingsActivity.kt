package com.yandex.maps.testapp.map

import android.app.AlertDialog
import android.os.Bundle
import android.widget.ProgressBar
import com.yandex.mapkit.RawTile
import com.yandex.mapkit.TileId
import com.yandex.mapkit.ZoomRange
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.geo.Projections
import com.yandex.mapkit.images.ImageUrlProvider
import com.yandex.mapkit.layers.Layer
import com.yandex.mapkit.layers.LayerLoadedListener
import com.yandex.mapkit.layers.LayerOptions
import com.yandex.mapkit.map.MapType
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.tiles.TileProvider
import com.yandex.maps.testapp.R
import com.yandex.maps.testapp.search.hide
import java.lang.Integer.min
import java.net.URL
import java.nio.charset.Charset
import java.util.logging.Logger
import kotlin.concurrent.thread

class RoadMarkingsActivity: MapBaseActivity() {
    private companion object {
        val LOGGER = Logger.getLogger("yandex.roadmarkings")!!
    }

    private var TILE_DATA_URL = "https://prototype.s3.yandex.net/roads/map.json"
    private var STYLE_URL = "https://prototype.s3.yandex.net/roads/styles.json"
    private var IMAGE_URL = "https://prototype.s3.yandex.net/roads/icons"

    private var downloadProgressBar: ProgressBar? = null

    private var layer: Layer? = null
    private var tileProvider: TileProvider? = null
    private var tileData: ByteArray? = null
    private var style: String? = null
    private var imageProvider: ImageUrlProvider = ImageUrlProvider {desc -> "${IMAGE_URL}/${desc.imageId}" }

    private fun createTestLayer() {
        thread {
            val progressCallback = { bytesRead: Int, bytesTotal: Int ->
                runOnUiThread {
                    downloadProgressBar!!.min = 0
                    downloadProgressBar!!.max = bytesTotal
                    downloadProgressBar!!.progress = bytesRead
                }
            }

            try {
                tileData = requestContent(TILE_DATA_URL, progressCallback)

                style = requestContent(STYLE_URL, progressCallback).toString(Charsets.UTF_8)
            }
            catch (e : Exception) {
                runOnUiThread {
                    var builder = AlertDialog.Builder(this)
                    builder.setTitle("Error")
                    builder.setMessage("Failed to download prototype data: " + e)
                    builder.create().show()
                }
                return@thread
            }

            runOnUiThread {
                downloadProgressBar!!.hide()

                tileProvider = TileProvider{tileId, version, etag ->
                    if (tileId.x == 154 && tileId.y == 80 && tileId.z == 8)
                        RawTile(version, etag, RawTile.State.OK, tileData!!)
                    else
                        RawTile(version, etag, RawTile.State.OK, emptyTile())
                }

                layer = mapview.map.addGeoJSONLayer(
                    "road_markings_layer",
                    style!!,
                    LayerOptions(),
                    tileProvider!!,
                    imageProvider,
                    Projections.getWgs84Mercator(),
                    arrayListOf(ZoomRange(8, 22)))
                layer?.invalidate("0")
                layer?.setLayerLoadedListener(layerLoadedListener)

                mapview.getMap().move(CameraPosition(Point(55.714105, 37.568380), 13.0f, 0.0f, 0.0f))
            }
        }

    }

    private val layerLoadedListener = object : LayerLoadedListener {
        override fun onLayerLoaded() {
            LOGGER.warning("Layer loaded!")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.road_markings)
        super.onCreate(savedInstanceState)
        super.setMapType(MapType.NONE)
        downloadProgressBar = findViewById(R.id.download_progress_bar)
        createTestLayer()
    }

    override fun vulkanPreferred(): Boolean {
        return true
    }

    private fun requestContent(urlString: String, progressCallback: ((Int, Int) -> Unit)? = null): ByteArray {
        val url = URL(urlString)
        val connection = url.openConnection()

        // Prevent the server from gzipping the data so that contentLength doesn't return -1
        // See https://stackoverflow.com/a/25349364
        connection.setRequestProperty("Accept-Encoding", "identity")
        connection.connect()
        val bytesTotal = connection.contentLength

        val inputStream = connection.getInputStream()

        var result = ByteArray(bytesTotal)
        var bytesRead = 0

        while (bytesRead < bytesTotal) {
            val count = inputStream.read(result, bytesRead, min(bytesTotal - bytesRead, 8192))
            if (count == -1) break
            bytesRead += count
            progressCallback?.invoke(bytesRead, bytesTotal)
        }

        return result
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
        """.trimIndent().toByteArray(Charsets.UTF_8)

}
