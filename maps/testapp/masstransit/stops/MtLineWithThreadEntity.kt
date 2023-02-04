package com.yandex.maps.testapp.masstransit.stops

import android.content.Context
import android.graphics.Color
import com.yandex.mapkit.transport.masstransit.Estimation
import com.yandex.mapkit.transport.masstransit.Line
import com.yandex.mapkit.transport.masstransit.ThreadAtStop

class MtLineWithThreadEntity(
    val line: Line,
    private val threadAtStop: ThreadAtStop?
) {
    val name: String; get() = line.name
    val uri: String?; get() = line.uri
    val id: String; get() = threadAtStop?.thread?.id ?: "none"
    val description: String; get() = threadAtStop?.thread?.description ?: "none"
    val types: String; get() = line.vehicleTypes.joinToString(";")

    fun color(): Int? {
        var color = line.style?.color ?: return null
        val blue = color % 256
        color /= 256
        val green = color % 256
        color /= 256
        val red = color % 256
        return Color.rgb(red, green, blue)
    }

    fun stops(): String {
        return threadAtStop?.thread?.essentialStops
            ?.takeIf { it.isNotEmpty() }
            ?.joinToString(" - ") ?: "none"
    }

    fun schedule(context: Context): String {
        return threadAtStop?.schedule?.scheduleEntries
            ?.map { EntriesFormatter.formatScheduleEntry(context, it) }
            ?.takeIf { it.isNotEmpty() }
            ?.joinToString(", ") ?: "none"
    }

    fun estimations(): List<Estimation> {
        val schedule = threadAtStop?.schedule
        val estimations = mutableListOf<Estimation>()
        if (schedule != null) {
            for (scheduleEntry in schedule.scheduleEntries) {
                val periodical = scheduleEntry.periodical
                if (periodical != null) {
                    estimations.addAll(periodical.estimations)
                }
                val scheduled = scheduleEntry.scheduled
                if (scheduled != null) {
                    val estimation = scheduled.estimation
                    if (estimation != null) {
                        estimations.add(estimation)
                    }
                }
            }
        }
        return estimations
    }
}
