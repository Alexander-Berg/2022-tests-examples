package com.yandex.maps.testapp.search

import android.content.Context
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.appyvet.materialrangebar.RangeBar
import com.yandex.maps.testapp.R

class RangeFilterControl(
    context: Context,
    val filter: RangeFilter
): LinearLayout(context)
{
    private val name by lazy { find<TextView>(R.id.name) }
    private val rangeBar by lazy { find<RangeBar>(R.id.range_bar) }
    private val fromText by lazy { find<TextView>(R.id.from) }
    private val toText by lazy { find<TextView>(R.id.to) }
    private var range: Range? = null

    private val rangeBarListener = object: RangeBar.OnRangeBarChangeListener {
        override fun onTouchEnded(rangeBar: RangeBar?) {}
        override fun onTouchStarted(rangeBar: RangeBar?) {}
        override fun onRangeChangeListener(rangeBar: RangeBar?,
                                           leftPinIndex: Int,
                                           rightPinIndex: Int,
                                           leftPinValue: String?,
                                           rightPinValue: String?) {
            range = leftPinValue?.toDoubleOrNull()?.let { from ->
                rightPinValue?.toDoubleOrNull()?.let { to ->
                    Range(from, to)
                }
            }
            setRangeText(range)
        }
    }

    private fun setRangeText(range: Range?) {
        range?.let {
            fromText.text = it.from.toInt().toString()
            toText.text = it.to.toInt().toString()
        }
    }

    init {
        LayoutInflater.from(context).inflate(
            R.layout.search_range_filter_control,
            this,
            true
        )

        name.text = filter.name
        // MIN_VALUE and MAX_VALUE are special cases in the backend response
        // which we do not want to handle
        rangeBar.isEnabled = !filter.disabled &&
            filter.range.from > Double.MIN_VALUE &&
            filter.range.to < Double.MAX_VALUE;
        if (rangeBar.isEnabled) {
            // RangeBar requires tickEnd - tickStart >= tickInterval so we set
            // tickEnd first as it is "safer". And also make tickInterval large
            // enough to generate reasonable amount of ticks.
            val tickInterval = (filter.range.to - filter.range.from) / 20
            while (rangeBar.tickInterval < tickInterval) {
                rangeBar.setTickInterval(rangeBar.tickEnd - rangeBar.tickStart)
                rangeBar.tickEnd *= 2
            }
            rangeBar.tickEnd = filter.range.to.toFloat()
            rangeBar.tickStart = filter.range.from.toFloat()
            rangeBar.setTickInterval(tickInterval.toFloat())
            rangeBar.setOnRangeBarChangeListener(rangeBarListener)
            // Default text formatter trims value to 4 chars only thus value
            // of 15000 is transformed to 1500. So we disable this behavior.
            rangeBar.setPinTextFormatter { it }
        }
        setRangeText(filter.range)
    }

    fun makeResultFilter() = range?.let {
        if (it != filter.range) {
            filter.copy(range = it)
        } else {
            null
        }
    }
}
