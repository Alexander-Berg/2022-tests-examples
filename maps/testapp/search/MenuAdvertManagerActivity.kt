package com.yandex.maps.testapp.search

import android.app.AlertDialog
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.search.*
import com.yandex.maps.testapp.R
import com.yandex.maps.testapp.TestAppActivity
import com.yandex.runtime.Error
import java.text.SimpleDateFormat
import java.util.*

class MenuAdvertManagerActivity : TestAppActivity() {

    companion object {
        private val CITY_POINTS = mapOf(
                R.id.menu_advert_manager_city_msk   to Point(55.756888, 37.615071),
                R.id.menu_advert_manager_city_spb   to Point(59.937128, 30.312444),
                -1                                  to Point(55.756888, 37.615071) // default - MSK
        )

        private var initialPointId: Int = R.id.menu_advert_manager_city_msk // hack to save value between activity instances
    }

    inner class MenuListAdapter : BaseAdapter() {

        override fun getItem(position: Int) = Object() // stub
        override fun getItemId(position: Int) = 0L // stub
        override fun getCount() = menuManager.menuInfo.menuItems.size

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val inflater = LayoutInflater.from(this@MenuAdvertManagerActivity)
            val view = inflater.inflate(R.layout.search_menu_advert_item, parent, false)

            val item = menuManager.menuInfo.menuItems[position]
            view.find<TextView>(R.id.search_menu_advert_item_title).text = item.title

            var details = "query: ${item.searchQuery}\n"
            details += "subtitle: ${item.subtitle}\n"
            details += "log id: ${item.logId}\n"

            details += "types: "
            for (type in item.types) {
                details += "[$type] "
            }
            details += "\n\n"

            for (image in item.images) {
                details += "image.urlTemplate: ${image.urlTemplate}\n"
            }

            val imageView = view.find<ImageView>(R.id.search_menu_advert_item_image)
            if (item.images.isNotEmpty()) {
                imageView.setImageBitmap(bitmapFor(position,
                        String.format(item.images.first().urlTemplate, "M")))
            } else {
                imageView.setImageResource(android.R.color.transparent)
            }

            for (property in item.properties) {
                details += "${property.key}: ${property.value}\n"
            }

            view.find<TextView>(R.id.search_menu_advert_item_details).text = details
            return view
        }
    }

    private val adapter = MenuListAdapter()

    private val advertPageId by lazy { getSearchOptions(this).advertPageId ?: "maps" }

    private val menuManager by lazy { SearchFactory.getInstance().createMenuManager(advertPageId) }

    private val imageDownloader by lazy {
        SearchFactory.getInstance().createImageDownloader(StorageCaching.DISABLED)
    }

    data class MenuImage(
            var session: ImageSession? = null,
            var bitmap: Bitmap? = null)

    private val images = HashMap<Int, MenuImage>()

    private val textView by lazy { find<TextView>(R.id.menu_advert_manager_text_view) }
    private val cities by lazy { find<RadioGroup>(R.id.menu_advert_manager_city_selector) }
    private val list by lazy { find<ListView>(R.id.menu_advert_manager_list_view) }

    private fun searchPoint() = CITY_POINTS[cities.checkedRadioButtonId]!!

    private val menuListener = MenuListener {
        informOnEmptyResults(
            this@MenuAdvertManagerActivity,
            menuManager.menuInfo.menuItems
        )
        for ((_, image) in images) {
            image.session = null
        }
        images.clear()

        textView.text = "Last update: " +
            SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().time)
        adapter.notifyDataSetChanged()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.search_menu_advert_manager)

        cities.check(initialPointId)

        list.adapter = adapter

        menuManager.addListener(menuListener)

        doSearch()
    }

    @Suppress("UNUSED_PARAMETER")
    fun onCitySelected(view: View) {
        initialPointId = cities.checkedRadioButtonId

        AlertDialog.Builder(this)
                .setTitle("Update takes some time")
                .setMessage("Wait ~30 min or go back and enter this menu again to update city info")
                .setPositiveButton("OK", null)
                .show()

        doSearch()
    }

    private fun doSearch() {
        menuManager.setPosition(searchPoint())
    }

    private fun bitmapFor(row: Int, id: String): Bitmap? {
        var image: MenuImage? = images[row]
        if (image != null) {
            return image.bitmap
        }

        image = MenuImage()
        images[row] = image
        image.session = imageDownloader.requestImage(id, object : ImageListener {
            override fun onImageReceived(bitmap: Bitmap) {
                image.session = null
                image.bitmap = bitmap
                adapter.notifyDataSetChanged()
            }

            override fun onImageError(error: Error) {
                image.session = null
            }
        })

        return null
    }

    override fun onStopImpl(){}
    override fun onStartImpl(){}

}
