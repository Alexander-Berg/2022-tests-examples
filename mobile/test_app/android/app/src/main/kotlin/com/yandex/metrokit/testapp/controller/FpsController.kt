package com.yandex.metrokit.testapp.controller

import android.widget.TextView
import com.yandex.metrokit.testapp.R

class FpsController(
        private val fps: () -> Float,
        private val view: TextView
) {
    private val updateFunc = object : Runnable {
        override fun run() {
            view.text = view.context.getString(R.string.fps_format).format(fps())
            view.postDelayed(this, PULL_INTERVAL)
        }
    }

    fun start() {
        view.removeCallbacks(updateFunc)
        updateFunc.run()
    }

    fun stop() {
        view.removeCallbacks(updateFunc)
    }

    private companion object {
        const val PULL_INTERVAL = 200L // milliseconds
    }
}
