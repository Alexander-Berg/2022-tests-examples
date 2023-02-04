package com.yandex.maps.testapp.masstransit.stops.schedule

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.yandex.mapkit.GeoObject
import com.yandex.mapkit.GeoObjectSession.GeoObjectListener
import com.yandex.mapkit.transport.TransportFactory
import com.yandex.mapkit.transport.masstransit.StopAlert
import com.yandex.mapkit.transport.masstransit.StopMetadata
import com.yandex.mapkit.transport.masstransit.StopScheduleMetadata
import com.yandex.maps.testapp.R
import com.yandex.maps.testapp.TestAppActivity
import com.yandex.maps.testapp.search.metadata
import com.yandex.runtime.Error
import kotlinx.android.synthetic.main.map_masstransit_stop_schedule.*
import java.util.logging.Logger

class MasstransitStopScheduleActivity : TestAppActivity(), GeoObjectListener {

    companion object {
        private val LOGGER = Logger.getLogger("yandex.maps")

        private const val DELAY_TIME_MS = 5000L
        private const val STOP_URI_KEY = "STOP_URI"
        private const val SCHEDULE_TIMESTAMP_KEY = "SCHEDULE_TIMESTAMP"

        @JvmStatic
        fun createIntent(
            context: Context,
            stopUri: String,
            timestampInMs: Long? = null
        ): Intent {
            return Intent(context, MasstransitStopScheduleActivity::class.java).apply {
                putExtra(STOP_URI_KEY, stopUri)
                timestampInMs?.let { putExtra(SCHEDULE_TIMESTAMP_KEY, it) }
            }
        }
    }

    private var timestamp: Long? = null
    private val mtInfoService by lazy { TransportFactory.getInstance().createMasstransitInfoService() }
    private val dataNotReadyMessageHandler = Handler()
    private val showDataNotReadyMessage = Runnable {
        masstransit_schedule_status.setText(R.string.masstransit_still_no_response)
    }
    private val onUriResolvedListener = object : GeoObjectListener {
        override fun onGeoObjectError(error: Error) {
            val message = "Failed to fetch stop uri"
            LOGGER.info(message)
            masstransit_schedule_status.text = message
        }

        override fun onGeoObjectResult(geoObject: GeoObject) {
            val stopId = geoObject.metadata<StopMetadata>()?.stop?.id
            if (stopId == null) {
                val message = "Couldn't resolve stop uri"
                LOGGER.severe(message)
                masstransit_schedule_status.text = message
                return
            }
            mtInfoService.schedule(stopId, timestamp, this@MasstransitStopScheduleActivity)
            dataNotReadyMessageHandler.postDelayed(showDataNotReadyMessage, DELAY_TIME_MS)
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.map_masstransit_stop_schedule)
        super.onCreate(savedInstanceState)
        val uriView = findViewById<View>(R.id.masstransit_schedule_id) as TextView
        val intent = intent
        if (intent.hasExtra(SCHEDULE_TIMESTAMP_KEY)) {
            timestamp = intent.getLongExtra(SCHEDULE_TIMESTAMP_KEY, 0L)
        }
        var uri = intent.getStringExtra(STOP_URI_KEY)
        uriView.text = uri
        if (uri == null) {
            LOGGER.severe("POI's full card must have not null uri specified")
            uri = ""
        }
        mtInfoService.resolveStopUri(uri, onUriResolvedListener)
        masstransit_schedule_status.text = "Resolving uri..."
    }

    override fun onGeoObjectError(error: Error) {
        dataNotReadyMessageHandler.removeCallbacks(showDataNotReadyMessage)
        val message = getString(R.string.masstransit_failed_to_get_data, error)
        LOGGER.info(message)
        masstransit_schedule_status.text = message
    }

    override fun onGeoObjectResult(geoObject: GeoObject) {
        dataNotReadyMessageHandler.removeCallbacks(showDataNotReadyMessage)
        val stopScheduleMetadata = geoObject.metadataContainer.getItem(StopScheduleMetadata::class.java)
        if (stopScheduleMetadata == null) {
            Toast.makeText(this, "No StopScheduleMetadata in response", Toast.LENGTH_SHORT).show()
            return
        }
        masstransit_schedule_name.text = stopScheduleMetadata.stop.name
        val effect = stopScheduleMetadata.alert?.effect
        if (effect != null) {
            masstransit_schedule_alert.text = when (effect) {
                StopAlert.Effect.NO_SERVICE -> "Alert: Stop is closed (Effect: ${effect.name})"
                else -> "Alert: Effect: ${effect.name}"
            }
        } else {
            masstransit_schedule_alert.visibility = View.GONE
        }
        val adapter = StopScheduleMetadataAdapter(mtInfoService, stopScheduleMetadata)
        masstransit_schedule_list.adapter = adapter
        masstransit_schedule_status.visibility = View.GONE
    }

    // should be implemented
    override fun onStopImpl() = Unit
    override fun onStartImpl() = Unit
}
