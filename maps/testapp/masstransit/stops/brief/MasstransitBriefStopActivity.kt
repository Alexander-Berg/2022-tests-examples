package com.yandex.maps.testapp.masstransit.stops.brief

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import com.yandex.mapkit.GeoObject
import com.yandex.mapkit.GeoObjectSession.GeoObjectListener
import com.yandex.mapkit.transport.TransportFactory
import com.yandex.mapkit.transport.masstransit.StopAlert
import com.yandex.mapkit.transport.masstransit.StopMetadata
import com.yandex.maps.testapp.R
import com.yandex.maps.testapp.TestAppActivity
import com.yandex.runtime.Error
import kotlinx.android.synthetic.main.map_masstransit_brief_stop.*
import java.util.logging.Logger

class MasstransitBriefStopActivity : TestAppActivity(), GeoObjectListener {
    companion object {
        private val LOGGER = Logger.getLogger("yandex.maps")

        private const val DELAY_TIME_MS = 5000L
        private const val STOP_URI_KEY = "STOP_URI"

        @JvmStatic
        fun createIntent(context: Context, stopUri: String): Intent {
            return Intent(context, MasstransitBriefStopActivity::class.java).apply {
                putExtra(STOP_URI_KEY, stopUri)
            }
        }
    }

    private val mtInfoService by lazy { TransportFactory.getInstance().createMasstransitInfoService() }
    private val dataNotReadyMessageHandler = Handler()
    private val showDataNotReadyMessage = Runnable {
        masstransit_brief_stop_status.setText(R.string.masstransit_still_no_response)
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.map_masstransit_brief_stop)
        super.onCreate(savedInstanceState)
        val intent = intent
        val uri = intent.getStringExtra(STOP_URI_KEY).orEmpty()
        masstransit_stop_id.text = uri
        if (uri.isEmpty()) {
            LOGGER.severe("POI's full card must have not null uri specified")
        }
        mtInfoService.resolveStopUri(uri, this)
        dataNotReadyMessageHandler.postDelayed(showDataNotReadyMessage, DELAY_TIME_MS)
    }

    override fun onGeoObjectError(error: Error) {
        dataNotReadyMessageHandler.removeCallbacks(showDataNotReadyMessage)
        val message = getString(R.string.masstransit_failed_to_get_data, error)
        LOGGER.info(message)
        masstransit_brief_stop_status.text = message
    }

    override fun onGeoObjectResult(geoObject: GeoObject) {
        dataNotReadyMessageHandler.removeCallbacks(showDataNotReadyMessage)
        val stopMetadata = geoObject.metadataContainer.getItem(StopMetadata::class.java)

        if (stopMetadata == null) {
            masstransit_brief_stop_status.text = "No StopMetadata in response"
            return
        }

        masstransit_stop_name.text = stopMetadata.stop.name
        val effect = stopMetadata.alert?.effect
        if (effect != null) {
            masstransit_brief_stop_alert.text = when (effect) {
                StopAlert.Effect.NO_SERVICE -> "Alert: Stop is closed (Effect: ${effect.name})"
                else -> "Alert: Effect: ${effect.name}"
            }
        } else {
            masstransit_brief_stop_alert.visibility = View.GONE
        }

        val adapter = StopMetadataAdapter(mtInfoService, stopMetadata)
        masstransit_brief_stop_list.adapter = adapter
        masstransit_brief_stop_status.visibility = View.GONE
    }

    override fun onStopImpl() = Unit
    override fun onStartImpl() = Unit
}
