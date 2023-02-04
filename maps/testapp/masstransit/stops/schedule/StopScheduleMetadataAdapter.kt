package com.yandex.maps.testapp.masstransit.stops.schedule

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.yandex.mapkit.transport.masstransit.MasstransitInfoService
import com.yandex.mapkit.transport.masstransit.StopScheduleMetadata
import com.yandex.maps.testapp.R
import com.yandex.maps.testapp.masstransit.stops.MtLineWithThreadEntity
import com.yandex.maps.testapp.masstransit.stops.MtLineWithThreadEntityViewHolder

private fun createEntries(metadata: StopScheduleMetadata): List<MtLineWithThreadEntity> {
    val entries = mutableListOf<MtLineWithThreadEntity>()
    for (lineAtStop in metadata.linesAtStop) {
        val threadsAtStop = lineAtStop.threadsAtStop
        if (threadsAtStop.isEmpty()) {
            entries.add(MtLineWithThreadEntity(lineAtStop.line, null))
            continue
        }
        for (threadAtStop in threadsAtStop) {
            entries.add(MtLineWithThreadEntity(lineAtStop.line, threadAtStop))
        }
    }
    return entries
}

class StopScheduleMetadataAdapter(
    private val mtInfoService: MasstransitInfoService,
    metadata: StopScheduleMetadata
) : BaseAdapter() {

    private val entries = createEntries(metadata)

    override fun getCount(): Int = entries.size
    override fun getItem(i: Int): MtLineWithThreadEntity = entries[i]
    override fun getItemId(i: Int): Long = i.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val entity = getItem(position)
        val view = convertView ?: LayoutInflater.from(parent.context).inflate(
            R.layout.map_masstransit_stop_item,
            parent,
            false
        )
        MtLineWithThreadEntityViewHolder(view).bind(entity, mtInfoService)
        return view
    }
}
