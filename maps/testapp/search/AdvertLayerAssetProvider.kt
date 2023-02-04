package com.yandex.maps.testapp.search

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.yandex.mapkit.GeoObject
import com.yandex.mapkit.search.BillboardObjectMetadata
import com.yandex.mapkit.search.advert_layer.AssetProvider
import com.yandex.mapkit.search.advert_layer.LabelPlacement
import com.yandex.maps.testapp.R
import com.yandex.runtime.image.ImageProvider

class AdvertLayerAssetProvider: AssetProvider {
    private val context = com.yandex.runtime.Runtime.getApplicationContext()

    private val colorLabelDay = ContextCompat.getColor(context, R.color.advert_layer_label_day)
    private val colorLabelNight = ContextCompat.getColor(context, R.color.advert_layer_label_night)
    private val colorTitleDay = ContextCompat.getColor(context, R.color.advert_layer_title_day)
    private val colorTitleNight = ContextCompat.getColor(context, R.color.advert_layer_title_night)
    private val colorSubtitleDay = ContextCompat.getColor(context, R.color.advert_layer_subtitle_day)
    private val colorSubtitleNight = ContextCompat.getColor(context, R.color.advert_layer_subtitle_night)

    override fun advertLabelImage(
        geoObject: GeoObject,
        nightMode: Boolean,
        placement: LabelPlacement): ImageProvider?
    {
        val properties = geoObject.metadata<BillboardObjectMetadata>()?.properties
        val title = properties?.find { it.key == "pinTitle" }?.value ?: return null
        val subtitle = properties?.find { it.key == "pinSubtitle" }?.value ?: return null

        val view = LayoutInflater.from(context).inflate(R.layout.search_advert_layer_label, null)

        view.findViewById<LinearLayout>(R.id.layout).gravity = when (placement) {
            LabelPlacement.LEFT -> Gravity.RIGHT
            LabelPlacement.RIGHT -> Gravity.LEFT
        }

        val background = DrawableCompat.wrap(view.background)
        DrawableCompat.setTint(background, if (nightMode) colorLabelNight else colorLabelDay)
        view.background = background

        view.findViewById<TextView>(R.id.title).apply {
            text = title
            setTextColor(if (nightMode) colorTitleNight else colorTitleDay)
        }
        view.findViewById<TextView>(R.id.subtitle).apply {
            text = subtitle
            setTextColor(if (nightMode) colorSubtitleNight else colorSubtitleDay)
        }

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
