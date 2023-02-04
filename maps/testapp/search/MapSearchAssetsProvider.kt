package com.yandex.maps.testapp.search

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PointF
import android.text.Html
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.Rect
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.search.search_layer.AssetsProvider
import com.yandex.mapkit.search.search_layer.PlacemarkIconType
import com.yandex.mapkit.search.search_layer.SearchResultItem
import com.yandex.mapkit.search.search_layer.Size
import com.yandex.maps.testapp.R
import com.yandex.runtime.image.ImageProvider

class MapSearchAssetsProvider (private val mapView: MapView): AssetsProvider {
    private val context = com.yandex.runtime.Runtime.getApplicationContext()

    private val ICON_STYLE_SELECTED = IconStyle()
        .setAnchor(PointF(0.5f, 0.8f))
        .setTappableArea(Rect(PointF(0f, 0f), PointF(0f, 0f)))
    private val ICON_STYLE_LABEL_LEFT = IconStyle().setAnchor(PointF(1f, 0.53f))
    private val ICON_STYLE_LABEL_RIGHT = IconStyle().setAnchor(PointF(0f, 0.53f))
    private val ICON_STYLE_DEFAULT = IconStyle().setAnchor(PointF(0.5f, 0.5f))

    private val selectedIcon by lazy { ImageProvider.fromResource(context, R.drawable.search_layer_pin_selected) }
    private val dustIcon by lazy { ImageProvider.fromResource(context, R.drawable.search_layer_pin_dust) }
    private val offlineDustIcon by lazy { ImageProvider.fromResource(context, R.drawable.search_layer_pin_dust_offline) }
    private val regularIcon by lazy { ImageProvider.fromResource(context, R.drawable.search_layer_pin_icon) }
    private val restaurantIcon by lazy { ImageProvider.fromResource(context, R.drawable.search_layer_pin_icon_restaurants) }
    private val offlineIcon by lazy { ImageProvider.fromResource(context, R.drawable.search_layer_pin_icon_offline) }
    private val visitedDustIcon by lazy { ImageProvider.fromResource(context, R.drawable.search_layer_pin_dust_visited) }
    private val visitedIcon by lazy { ImageProvider.fromResource(context, R.drawable.search_layer_pin_icon_visited) }
    private val visitedRestaurantIcon by lazy { ImageProvider.fromResource(context, R.drawable.search_layer_pin_icon_restaurants_visited) }
    private val visitedOfflineDustIcon by lazy { ImageProvider.fromResource(context, R.drawable.search_layer_pin_dust_offline_visited) }
    private val visitedOfflineIcon by lazy { ImageProvider.fromResource(context, R.drawable.search_layer_pin_icon_offline_visited) }
    private val advertisingIcon by lazy { ImageProvider.fromResource(context, R.drawable.search_layer_pin_icon_advertising) }
    private val visitedAdvertisingIcon by lazy { ImageProvider.fromResource(context, R.drawable.search_layer_pin_icon_advertising_visited) }
    private val advertisingDustIcon by lazy { ImageProvider.fromResource(context, R.drawable.search_layer_pin_dust_advertising) }
    private val visitedAdvertisingDustIcon by lazy { ImageProvider.fromResource(context, R.drawable.search_layer_pin_dust_advertising_visited) }

    @get:Synchronized @set:Synchronized
    var highlightAdvert = true

    private fun getDustIcon(searchResult: SearchResultItem): ImageProvider {
       return when {
           searchResult.isHighlighted && highlightAdvert -> advertisingDustIcon
           searchResult.isOffline -> offlineDustIcon
           else -> dustIcon
       }
    }

    private fun getVisitedDustIcon(searchResult: SearchResultItem): ImageProvider {
        return when {
            searchResult.isHighlighted && highlightAdvert -> visitedAdvertisingDustIcon
            searchResult.isOffline -> visitedOfflineDustIcon
            else -> visitedDustIcon
        }
    }

    private fun getIcon(searchResult: SearchResultItem): ImageProvider {
        return when {
            searchResult.isHighlighted && highlightAdvert -> advertisingIcon
            searchResult.isOffline -> offlineIcon
            searchResult.categoryClass == "restaurants" -> restaurantIcon
            else -> regularIcon
        }
    }

    private fun getVisitedIcon(searchResult: SearchResultItem): ImageProvider {
        return when {
            searchResult.isHighlighted && highlightAdvert -> visitedAdvertisingIcon
            searchResult.isOffline -> visitedOfflineIcon
            searchResult.categoryClass == "restaurants" -> visitedRestaurantIcon
            else -> visitedIcon
        }
    }

    override fun image(searchResult: SearchResultItem, placemarkIconType: Int): ImageProvider {
        val type = PlacemarkIconType.values()[placemarkIconType]
        return when {
            type == PlacemarkIconType.SELECTED -> selectedIcon

            type == PlacemarkIconType.DUST -> getDustIcon(searchResult)
            type == PlacemarkIconType.DUST_VISITED -> getVisitedDustIcon(searchResult)

            type == PlacemarkIconType.ICON -> getIcon(searchResult)
            type == PlacemarkIconType.ICON_VISITED -> getVisitedIcon(searchResult)

            type == PlacemarkIconType.LABEL_SHORT_LEFT -> shortLabelImage(searchResult, isLeft = true)
            type == PlacemarkIconType.LABEL_SHORT_RIGHT -> shortLabelImage(searchResult, isLeft = false)
            isDetailedLabel(type) -> detailedLabelImage(searchResult)

            else -> regularIcon
        }
    }

    private fun density() = mapView.resources.displayMetrics.density

    override fun iconStyle(searchResult: SearchResultItem, placemarkIconType: Int): IconStyle {
        val type = PlacemarkIconType.values()[placemarkIconType]
        return when {
            type == PlacemarkIconType.SELECTED -> ICON_STYLE_SELECTED
            isRightLabel(type) -> ICON_STYLE_LABEL_RIGHT
            isLeftLabel(type) -> ICON_STYLE_LABEL_LEFT
            else -> ICON_STYLE_DEFAULT
        }.setScale(mapView.scaleFactor / density())
    }

    override fun size(searchResult: SearchResultItem, placemarkIconType: Int): Size {
        val img = image(searchResult, placemarkIconType)
        val scale = density().toDouble()

        return Size(img.image.width / scale, img.image.height / scale)
    }

    override fun canProvideLabels(searchResult: SearchResultItem) = true

    private fun isRightLabel(type: PlacemarkIconType?): Boolean {
        return when (type) {
            PlacemarkIconType.LABEL_SHORT_RIGHT -> true
            PlacemarkIconType.LABEL_DETAILED_RIGHT -> true
            else -> false
        }
    }

    private fun isLeftLabel(type: PlacemarkIconType?): Boolean {
        return when (type) {
            PlacemarkIconType.LABEL_SHORT_LEFT -> true
            PlacemarkIconType.LABEL_DETAILED_LEFT -> true
            else -> false
        }
    }

    private fun isDetailedLabel(type: PlacemarkIconType?): Boolean {
        return when (type) {
            PlacemarkIconType.LABEL_DETAILED_RIGHT -> true
            PlacemarkIconType.LABEL_DETAILED_LEFT -> true
            else -> false
        }
    }

    private fun shortLabelImage(item: SearchResultItem?, isLeft: Boolean): ImageProvider {
        val layoutInflater = LayoutInflater.from(context)

        val view = layoutInflater.inflate(
                if (isLeft) R.layout.search_layer_label_short_left else R.layout.search_layer_label_short_right,
                null
        ) as TextView
        view.text = item?.geoObject?.name
        view.measure(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        val bitmap = Bitmap.createBitmap(
                view.measuredWidth,
                view.measuredHeight,
                Bitmap.Config.ARGB_8888
        )
        view.draw(Canvas(bitmap))
        return ImageProvider.fromBitmap(bitmap)
    }

    private fun detailedLabelImage(item: SearchResultItem?) : ImageProvider {
        val layoutInflater = LayoutInflater.from(context)

        val view = layoutInflater.inflate(R.layout.search_layer_label_detailed,null)

        val nameView = view.findViewById<TextView>(R.id.name)
        val detailView = view.findViewById<TextView>(R.id.info)
        nameView.text = item?.geoObject?.name
        detailView.text = Html.fromHtml(item?.details()?.map{
            when {
                it.type == "rating" -> "<b>★%s</b>".format(
                    it.properties.find { it.key == "value_5" }?.value ?: it.text
                )
                it.type == "travel_time" ->
                    if (it.properties.find{ it.key == "type" }?.value != "Walking") ""
                    else "пешком " + it.properties.find{ it.key == "text" }?.value
                it.type == "star" -> it.properties.find{ it.key == "id" }?.value
                else -> it.text
            }
        }?.joinToString(separator = " "))

        view.measure(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        val bitmap = Bitmap.createBitmap(
                view.measuredWidth,
                view.measuredHeight,
                Bitmap.Config.ARGB_8888
        )
        view.draw(Canvas(bitmap))
        return ImageProvider.fromBitmap(bitmap)
    }
}
