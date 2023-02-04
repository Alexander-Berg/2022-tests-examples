package com.yandex.maps.testapp.search

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import com.yandex.mapkit.GeoObject
import com.yandex.mapkit.GeoObjectCollection
import com.yandex.mapkit.search.search_layer.SearchResultItem
import com.yandex.maps.testapp.R

class SearchResultsBox(context: Context, attributes: AttributeSet) : LinearLayout(context, attributes) {
    private val geoObjects = mutableListOf<GeoObjectCollection.Item>()
    private val geoObjectIds = mutableListOf<String>()

    private val adapter = SearchResultAdapter(context, geoObjects)
    private val hasMoreView = ProgressBar(context)

    var fetchMoreFunction: (() -> Unit)? = null
    var onItemClickFunction : ((String, GeoObject) -> Unit)? = null

    var hasMore = true
        set(value) {
            hasMoreView.visibility = if (value) View.VISIBLE else View.INVISIBLE
            field = value
        }

    init {
        LayoutInflater.from(context).inflate(R.layout.search_results, this, true)
        orientation = VERTICAL

        setupView()
    }

    fun clear() {
        geoObjects.clear()
        adapter.notifyDataSetChanged()
    }

    fun append(newObjects: Collection<GeoObjectCollection.Item>) {
        geoObjects.addAll(newObjects)
        geoObjectIds.addAll(newObjects.map{""})
        adapter.notifyDataSetChanged()
    }

    fun append(newObject: GeoObject) {
        geoObjects.add(GeoObjectCollection.Item.fromObj(newObject))
        geoObjectIds.add("")
        adapter.notifyDataSetChanged()
    }

    fun append(newObject: SearchResultItem) {
        geoObjects.add(GeoObjectCollection.Item.fromObj(newObject.geoObject))
        geoObjectIds.add(newObject.id)
        adapter.notifyDataSetChanged()
    }

    private fun setupView() {
        val view = find<ListView>(R.id.search_results_list)
        view.addFooterView(hasMoreView)
        hasMore = false

        view.adapter = adapter
        view.setOnScrollListener(object : AbsListView.OnScrollListener {
            override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {}
            override fun onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int,
                                  totalItemCount: Int) {
                if (hasMore && firstVisibleItem + visibleItemCount == totalItemCount) {
                    fetchMoreFunction?.invoke()
                }
            }
        })

        view.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            onItemClickFunction?.invoke(geoObjectIds[position], geoObjects[position].obj!!)
        }
    }
}
