package com.yandex.maps.testapp.search

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.yandex.mapkit.GeoObjectCollection

class SearchResultAdapter(
    val context: Context,
    private val geoObjects: List<GeoObjectCollection.Item>) : BaseAdapter() {

    override fun getItem(position: Int) = Object() // stub
    override fun getItemId(position: Int) = 0L // stub
    override fun getCount() = geoObjects.size

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        return SearchResultView(context, geoObjects[position].obj!!)
    }
}
