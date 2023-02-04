package com.yandex.maps.testapp.masstransit.stops

import android.content.Context
import com.yandex.mapkit.transport.masstransit.Estimation
import com.yandex.mapkit.transport.masstransit.Periodical
import com.yandex.mapkit.transport.masstransit.Schedule
import com.yandex.mapkit.transport.masstransit.Scheduled
import com.yandex.maps.testapp.R
import java.util.*

object EntriesFormatter {

    fun formatScheduleEntry(context: Context, scheduleEntry: Schedule.ScheduleEntry): String {
        val periodical = scheduleEntry.periodical
        if (periodical != null) {
            return formatPeriodical(context, periodical)
        }
        val scheduled = scheduleEntry.scheduled
        return scheduled?.let(this::formatScheduled).orEmpty()
    }

    fun formatEstimations(estimations: List<Estimation>): String {
        val estimationStrings: MutableList<String?> = ArrayList()
        for (estimation in estimations) estimationStrings.add(formatEstimation(estimation))
        return if (estimations.isNotEmpty()) estimationStrings.joinToString(", ") else "none"
    }

    private fun formatScheduled(scheduled: Scheduled): String {
        val arrival = scheduled.arrivalTime?.text ?: "-"
        val departure = scheduled.departureTime?.text ?: "-"
        return "$arrival/$departure"
    }

    private fun formatEstimation(estimation: Estimation): String {
        val id = estimation.vehicleId.orEmpty()
        val arrival = estimation.arrivalTime?.text ?: "-"
        return "$id at $arrival"
    }

    private fun formatPeriodical(context: Context, periodical: Periodical): String {
        val begin = periodical.begin?.text ?: "-"
        val end = periodical.end?.text ?: "-"
        return context.getString(R.string.masstransit_interval, begin, end, periodical.frequency.text)
    }
}
