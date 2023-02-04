package com.yandex.maps.testapp.search

import android.content.Context
import android.view.LayoutInflater
import android.widget.RelativeLayout
import android.widget.TextView
import com.yandex.mapkit.GeoObject
import com.yandex.mapkit.search.*
import com.yandex.maps.testapp.R

private const val CIRCLED_LETTER_D = "\uD83C\uDD53"
private const val SQUARED_LETTER_G = "\uD83C\uDD76"

private const val NO_ENTRY_EMOJI = "\u26D4"
private const val SYNAGOGUE_EMOJI = "\uD83D\uDD4D"
private const val PUSHPIN_EMOJI = "\uD83D\uDCCC"

// These are for future use when multiple "base" metadata will be available
@Suppress("unused")
private const val MOUNTAIN_EMOJI = "\u26F0\uFE0F"
@Suppress("unused")
private const val BRIEFCASE_EMOJI = "\uD83D\uDCBC"
@Suppress("unused")
private const val AERIAL_TRAMWAY_EMOJI = "\uD83D\uDEA1"

class SearchResultView(context: Context) : RelativeLayout(context) {
    init {
        val inflater = context.systemService<LayoutInflater>(Context.LAYOUT_INFLATER_SERVICE)
        inflater.inflate(R.layout.search_result, this, true)
    }

    constructor(context: Context, obj: GeoObject) : this(context) {
        setText(R.id.search_result_name, nameText(obj))
        setText(R.id.search_result_details, detailsText(obj))
        setText(R.id.search_result_distance, distanceText(obj))
    }

    private fun nameText(obj: GeoObject): String {
        var prefix = ""
        obj.metadata<BusinessObjectMetadata>()?.advertisement?.let {
            prefix += SQUARED_LETTER_G
        }
        obj.metadata<DirectObjectMetadata>()?.let {
            prefix += CIRCLED_LETTER_D
        }
        if (prefix.isNotEmpty()) { prefix += " " }

        var suffix = ""
        obj.metadata<MassTransitObjectMetadata>()?.let {
            suffix += NO_ENTRY_EMOJI
        }
        obj.metadata<POIObjectMetadata>()?.let {
            suffix += SYNAGOGUE_EMOJI
        }
        obj.metadata<CollectionEntryMetadata>()?.let {
            suffix += PUSHPIN_EMOJI
        }
        if (suffix.isNotEmpty()) { suffix = " $suffix" }
        
        val text = obj.metadata<CollectionEntryMetadata>()?.title
            ?: obj.name
            ?: ""

        return "$prefix${text}$suffix"
    }

    private fun detailsText(obj: GeoObject): String {
        return obj.metadata<CollectionEntryMetadata>()?.annotation
            ?: obj.metadata<DirectObjectMetadata>()?.text
            ?: obj.descriptionText
            ?: ""
    }

    private fun distanceText(obj: GeoObject) =
        obj.metadata<BusinessObjectMetadata>()?.distance?.text ?: ""

    private fun setText(id: Int, text: String?) {
        find<TextView>(id).text = text ?: ""
    }
}
