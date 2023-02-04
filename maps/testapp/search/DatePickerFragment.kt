package com.yandex.maps.testapp.search

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.DialogFragment
import android.os.Bundle
import android.widget.DatePicker
import java.util.*

class DatePickerFragment
    : DialogFragment()
    , DatePickerDialog.OnDateSetListener
{
    var onDateSetCallback: ((Int, Int, Int) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val calendar = Calendar.getInstance()
        val result = DatePickerDialog(activity,
            this,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        result.datePicker.minDate = calendar.timeInMillis
        return result
    }

    override fun onDateSet(datePicker: DatePicker?, year: Int, month: Int, day: Int) {
        onDateSetCallback?.let { it (year, month, day) }
    }
}