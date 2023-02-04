package com.yandex.maps.testapp.search

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.yandex.maps.testapp.R

const val BUTTON_TEXT_TEMPLATE = "Date %s: %04d-%02d-%02d"
const val PARAM_TEMPLATE = "%04d%02d%02d"

class DateFilterControl(
    context: Context,
    val filter: DateFilter
): LinearLayout(context)
{
    private val dateFromButton by lazy { find<Button>(R.id.from) }
    private val dateFromPicker: DatePickerFragment by lazy {
        DatePickerFragment().apply {
            onDateSetCallback = { year, month, day ->
                dateFromButton.text = BUTTON_TEXT_TEMPLATE.format(
                    "from", year, month + 1, day
                )
                dateFrom = PARAM_TEMPLATE.format(year, month + 1, day)
            }
        }
    }

    private val dateToButton by lazy { find<Button>(R.id.to) }
    private val dateToPicker: DatePickerFragment by lazy {
        DatePickerFragment().apply {
            onDateSetCallback = { year, month, day ->
                dateToButton.text = BUTTON_TEXT_TEMPLATE.format(
                    "to", year, month + 1, day
                )
                dateTo = PARAM_TEMPLATE.format(year, month + 1, day)
            }
        }
    }

    private var dateFrom: String? = null
    private var dateTo: String? = null

    init {
        LayoutInflater.from(context).inflate(
            R.layout.search_date_filter_control,
            this,
            true
        )
        find<TextView>(R.id.name).text = filter.name
        dateFromButton.setOnClickListener {
            dateFromPicker.show(
                (context as Activity).fragmentManager,
                "date-picker-from"
            )
        }
        dateToButton.setOnClickListener {
            dateToPicker.show(
                (context as Activity).fragmentManager,
                "date-picker-to"
            )
        }
    }

    fun makeResultFilter() = dateFrom?.let { from ->
        dateTo?.let { to ->
            if (from <= to)
                filter.copy(range=DateRange(from, to))
            else
                null
        }
    }
}
