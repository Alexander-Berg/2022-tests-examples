package com.yandex.maps.testapp.search

import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import com.yandex.maps.testapp.TestAppActivity

class TextListActivity : TestAppActivity() {

    companion object {
        const val TEXT_LIST_EXTRA = "list"
    }

    private var list = listOf<String>()

    inner class TextListAdapter : BaseAdapter() {

        override fun getItem(position: Int) = Object()
        override fun getItemId(position: Int) = 0L
        override fun getCount() = list.size

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

            val textView = TextView(this@TextListActivity)
            val params = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.setMargins(10, 8, 10, 8)
            textView.layoutParams = params
            textView.typeface = Typeface.MONOSPACE
            textView.text = list[position]

            val itemView = LinearLayout(this@TextListActivity)
            itemView.addView(textView)

            return itemView
        }
    }

    private val adapter = TextListAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        list = intent.getStringArrayExtra(TEXT_LIST_EXTRA)!!.toList()

        val listView = ListView(this)
        listView.adapter = adapter

        val params = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        setContentView(listView, params)
    }

    override fun onStopImpl() {}
    override fun onStartImpl() {}
}
