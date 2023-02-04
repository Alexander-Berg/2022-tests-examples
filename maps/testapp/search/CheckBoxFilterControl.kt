package com.yandex.maps.testapp.search

import android.content.Context
import android.widget.CheckBox

class CheckBoxFilterControl(context: Context, val filter: Filter): CheckBox(context) {
    init {
        text = filter.name
        isChecked = filter.selected
        isEnabled = !filter.disabled
    }
    fun makeResultFilter() = if (isChecked) filter else null
}