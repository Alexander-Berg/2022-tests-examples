package com.yandex.metrokit.testapp.controller

import android.view.View
import android.widget.ProgressBar
import kotlin.math.roundToInt

class ProgressController(private val progress: ProgressBar) {

    private var counter: Int = 0

    fun show() {
        if (counter == 0) {
            doShow()
        }
        ++counter
    }

    fun hide() {
        if (counter > 0) {
            --counter
            if (counter == 0) {
                doHide()
            }
        }
    }

    fun setProgress(value: Float) {
        progress.progress = (value * progress.max).roundToInt()
    }

    private fun doShow() {
        progress.visibility = View.VISIBLE
    }

    private fun doHide() {
        progress.visibility = View.GONE
    }
}
