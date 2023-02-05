package com.yandex.metrokit.testapp

import android.content.Context
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.viewpager.widget.PagerAdapter
import com.yandex.metrokit.scheme.data.routing.Route

class RouteAdapter(
        context: Context,
        private val routes: List<Route>
) : PagerAdapter() {
    private val resources: Resources = context.resources
    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view = inflater.inflate(R.layout.route_pager_item, container, false)
        bindView(view, position)
        container.addView(view)
        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, view: Any) {
        container.removeView(view as View)
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun getCount(): Int = routes.size

    private fun bindView(view: View, position: Int) {
        val route = routes[position]

        val time = resources.getString(R.string.route_time, route.time / 60.0)
        val transferCount = resources.getString(R.string.route_transfer_count, route.transfersCount)

        view.findViewById<TextView>(R.id.time).text = time
        view.findViewById<TextView>(R.id.transfer_count).text = transferCount
    }
}
