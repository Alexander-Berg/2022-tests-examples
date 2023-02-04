package com.yandex.maps.testapp.search

import android.content.Context
import android.util.AttributeSet
import android.widget.ListView

class SectionedListView(context: Context, attr: AttributeSet) : ListView(context, attr) {
    val adapter = SectionedListAdapter(context)
    init {
        setAdapter(adapter)
        setOnItemClickListener { _, _, position, _ ->
            adapter.getItem(position).onClick?.invoke()
        }
    }

    fun addSection(title: String, text: String?) {
        adapter.add(SectionedListAdapter.CellInfo(title, text ?: "null", ""))
    }

    fun addSection(title: String, items: Collection<ItemWithDetails>) {
        adapter.addAll(items.mapIndexed { i, item ->
            SectionedListAdapter.CellInfo(if (i == 0) title else null, item)
        })
    }

    fun addSection(title: String, vararg items: ItemWithDetails) {
        addSection(title, items.asList())
    }

    fun clearItems() = adapter.clearAll()
}

