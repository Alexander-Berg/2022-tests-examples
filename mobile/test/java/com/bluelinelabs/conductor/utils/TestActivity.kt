package com.bluelinelabs.conductor.utils

import android.app.Activity
import android.os.Bundle
import android.widget.FrameLayout

const val ROOT_ID = 42

class TestActivity : Activity() {

    var bundle: Bundle? = null
    var shouldChangingConfigurations = false
    var isDestroying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        bundle = savedInstanceState
        super.onCreate(savedInstanceState)

        val root = FrameLayout(this)
        root.id = ROOT_ID
        setContentView(root)
    }

    override fun isChangingConfigurations(): Boolean {
        return shouldChangingConfigurations
    }

    override fun isDestroyed(): Boolean {
        return isDestroying || super.isDestroyed()
    }
}
