package com.yandex.maps.testapp.masstransit.stops

import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.yandex.mapkit.transport.masstransit.*
import com.yandex.maps.testapp.R
import com.yandex.runtime.Error

class MtLineWithThreadEntityViewHolder(private val view: View) {
    private val context = view.context
    private val name = view.findViewById<TextView>(R.id.masstransit_stop_name)
    private val description = view.findViewById<TextView>(R.id.masstransit_stop_description)
    private val id = view.findViewById<TextView>(R.id.masstransit_stop_id)
    private val types = view.findViewById<TextView>(R.id.masstransit_stop_types)
    private val style = view.findViewById<TextView>(R.id.masstransit_stop_style)
    private val isNight = view.findViewById<TextView>(R.id.masstransit_stop_is_night)
    private val essentialStops = view.findViewById<TextView>(R.id.masstransit_stop_essential_stops)
    private val schedule = view.findViewById<TextView>(R.id.masstransit_stop_schedule)
    private val estimations = view.findViewById<TextView>(R.id.masstransit_stop_estimations)
    private val uriField = view.findViewById<TextView>(R.id.masstransit_stop_uri)

    fun bind(entity: MtLineWithThreadEntity, mtInfoService: MasstransitInfoService) {
        name.text = entity.name
        description.text = context.getString(R.string.masstransit_description, entity.description)
        id.text = context.getString(R.string.masstransit_id, entity.id)
        types.text = context.getString(R.string.masstransit_types, entity.types)

        entity.color().let { color ->
            if (color == null) {
                style.visibility = View.GONE
            } else {
                style.setBackgroundColor(color)
            }
        }
        isNight.text = context.getString(R.string.masstransit_night, entity.line.isNight)
        essentialStops.text = context.getString(R.string.masstransit_stops, entity.stops())
        schedule.text = context.getString(R.string.masstransit_schedule, entity.schedule(context))
        estimations.text = context.getString(R.string.masstransit_estimations, EntriesFormatter.formatEstimations(entity.estimations()))

        view.setOnClickListener {
            val estimations: List<Estimation> = entity.estimations()
            if (estimations.isEmpty()) {
                Toast.makeText(context, R.string.masstransit_no_estimations, Toast.LENGTH_SHORT).show()
            } else {
                val firstVehicleId = estimations.first().vehicleId!!
                mtInfoService.vehicle(firstVehicleId, object : VehicleSession.VehicleListener {
                    override fun onVehicleResponse(vehicle: Vehicle) {
                        Toast.makeText(
                            context,
                            context.getString(
                                R.string.masstransit_vehicle_is_located,
                                vehicle.id,
                                vehicle.position.longitude,
                                vehicle.position.latitude
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    override fun onVehicleError(error: Error) {
                        Toast.makeText(
                            context,
                            R.string.masstransit_could_not_fetch_vehicle_info,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
            }
        }

        uriField.text = context.getString(R.string.masstransit_line_uri, entity.uri)
        val uri = entity.uri ?: return
        uriField.setOnClickListener {
            mtInfoService.resolveLineUri(uri, object : LineSession.LineListener {
                override fun onLineResponse(lineInfo: LineInfo) {
                    val text = if (uri == lineInfo.line.uri) {
                        "Successfully resolved line with id ${lineInfo.line.id}"
                    } else {
                        "Bad resolve"
                    }
                    Toast.makeText(context.applicationContext, text, Toast.LENGTH_SHORT).show()
                }

                override fun onLineError(error: Error) {
                    Toast.makeText(
                        context.applicationContext,
                        "Error: $error",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }
    }
}
