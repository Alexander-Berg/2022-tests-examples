package com.yandex.maps.testapp.search

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.yandex.maps.testapp.R

class SectionedListAdapter(private val context: Context) : BaseAdapter() {
    private val items = mutableListOf<CellInfo>()

    data class CellInfo(val header: String?, val text: String?, val detail: String, val onClick: (() -> Unit)? = null) {
        constructor(title: String?, item: ItemWithDetails)
            : this(title, item.text ?: "(null)", item.details ?: "", item.onClick)
    }

    fun clearAll() {
        items.clear()
        notifyDataSetChanged()
    }

    private fun setupRowView(convertView: View?): View {
        if (convertView != null) {
            return convertView
        }
        val rowView = LayoutInflater
            .from(context)
            .inflate(R.layout.sectioned_list_cell, null)
        rowView.tag = ViewHolder(
            rowView.find(R.id.separator),
            rowView.find(R.id.title),
            rowView.find(R.id.subtitle)
        )
        return rowView
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val rowView = setupRowView(convertView)
        val viewHolder = rowView.tag as ViewHolder
        val cellInfo = items[position]
        if (cellInfo.header != null) {
            viewHolder.header.text = cellInfo.header
            viewHolder.header.show()
        } else {
            viewHolder.header.hide()
        }

        viewHolder.title.text = cellInfo.text
        viewHolder.subtitle.text = cellInfo.detail
        if (TextUtils.isEmpty(cellInfo.detail)) {
            viewHolder.subtitle.hide()
        } else {
            viewHolder.subtitle.show()
        }

        return rowView
    }

    override fun getCount() = items.size

    override fun getItem(position: Int) = items[position]

    override fun getItemId(position: Int) = position.toLong()

    fun add(cellInfo: CellInfo) {
        items.add(cellInfo)
        notifyDataSetChanged()
    }

    fun addAll(items: Collection<CellInfo>) {
        this.items.addAll(items)
        notifyDataSetChanged()
    }
}
