package com.yandex.maps.testapp.search

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner

class SpinnerWithPrompt(context: Context, attributeSet: AttributeSet) : Spinner(
    context,
    attributeSet
) {
    class Adapter(context: Context, resourceId: Int)
        : ArrayAdapter<Any>(
        context,
        android.R.layout.simple_spinner_item,
        context.resources.getStringArray(resourceId)
    )
    {
        init {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        override fun getCount(): Int {
            val count = super.getCount()
            return if (count > 0) count - 1 else count
        }
    }

    var onItemSelectedCallback: (() -> Unit)? = null

    fun resetToPrompt() = setSelection(adapter.count)

    fun hasValidSelection() = selectedItemPosition < adapter.count

    private val spinnerListener = object: OnItemSelectedListener, OnTouchListener {
        var userInteracting = false

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
            userInteracting = true
            return false
        }

        override fun onNothingSelected(p0: AdapterView<*>?) {}

        override fun onItemSelected(adapterView: AdapterView<*>?,
                                    p1: View?,
                                    position: Int,
                                    p3: Long) {
            if (!userInteracting) { return }
            onItemSelectedCallback?.let { it() }
            userInteracting = false
        }
    }

    init {
        onItemSelectedListener = spinnerListener
        setOnTouchListener(spinnerListener)
    }
}