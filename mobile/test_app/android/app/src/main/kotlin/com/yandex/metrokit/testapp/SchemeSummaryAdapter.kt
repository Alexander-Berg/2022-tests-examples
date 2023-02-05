package com.yandex.metrokit.testapp

import android.content.res.Resources
import android.database.DataSetObservable
import android.database.DataSetObserver
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListAdapter
import android.widget.SpinnerAdapter
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.yandex.metrokit.L10nManager
import com.yandex.metrokit.Language
import com.yandex.metrokit.scheme.SchemeSummary
import com.yandex.metrokit.scheme.UpdateInfo
import com.yandex.metrokit.testapp.common.formatByteSize
import java.util.Locale

class SchemeSummaryAdapter(
        private val layoutInflater: LayoutInflater,
        private val itemClickListener: ItemClickListener,
        var data: MutableList<SchemeSummary> = mutableListOf(),
        private val language: Language = Language(Locale.getDefault().language)
) : RecyclerView.Adapter<SchemeSummaryAdapter.ViewHolder>(), ListAdapter, SpinnerAdapter {

    private val dataSetObservable = DataSetObservable()

    interface ItemClickListener {
        fun onUpdateClick(position: Int, schemeSummary: SchemeSummary)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val name: TextView  = itemView.findViewById(R.id.name)
        private val info: TextView  = itemView.findViewById(R.id.info)
        private val id: TextView  = itemView.findViewById(R.id.id)
        private val version: TextView  = itemView.findViewById(R.id.version)
        private val tags: TextView  = itemView.findViewById(R.id.tags)
        private val update: TextView  = itemView.findViewById(R.id.update)

        fun bind(schemeSummary: SchemeSummary) {
            val res: Resources = itemView.context.resources

            name.text = L10nManager.getStringWithFallback(schemeSummary.name, language)
            id.text = res.getString(R.string.scheme_id, schemeSummary.schemeId.value)
            version.text = res.getString(R.string.scheme_version, schemeSummary.version)
            tags.text = res.getString(R.string.scheme_tags, schemeSummary.tags.joinToString())

            val updateInfo: UpdateInfo? = schemeSummary.updateInfo

            when {
                updateInfo != null -> {
                    val formattedByteSize = formatByteSize(res, updateInfo.downloadSize)

                    info.visibility = View.GONE

                    update.visibility = View.VISIBLE
                    update.text =
                            if (schemeSummary.isLocal) {
                                res.getString(R.string.scheme_update, formattedByteSize)
                            } else {
                                res.getString(R.string.scheme_download, formattedByteSize)
                            }
                }
                else -> {
                    info.visibility = View.GONE
                    update.visibility = View.GONE
                }
            }

            update.setOnClickListener {
                itemClickListener.onUpdateClick(adapterPosition, schemeSummary)
            }
        }
    }

    override fun getItemCount(): Int = data.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(layoutInflater.inflate(R.layout.scheme_summary_list_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(data[position])
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val viewHolder: ViewHolder = if (convertView == null) {
            val viewHolder = onCreateViewHolder(parent, 0)
            viewHolder.itemView.tag = viewHolder
            viewHolder
        } else {
            convertView.tag as ViewHolder
        }

        onBindViewHolder(viewHolder, position)

        return viewHolder.itemView
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getView(position, convertView, parent)
    }

    override fun getCount(): Int = data.size

    override fun isEmpty(): Boolean = data.isEmpty()

    override fun getItem(position: Int): Any = data[position]

    override fun getViewTypeCount(): Int = 1

    override fun isEnabled(position: Int): Boolean = true

    override fun areAllItemsEnabled(): Boolean = true

    override fun registerDataSetObserver(observer: DataSetObserver?) {
        dataSetObservable.registerObserver(observer)
    }

    override fun unregisterDataSetObserver(observer: DataSetObserver?) {
        dataSetObservable.unregisterObserver(observer)
    }

    fun notifyDataChanged() {
        notifyDataSetChanged()
        dataSetObservable.notifyChanged()
    }
}


