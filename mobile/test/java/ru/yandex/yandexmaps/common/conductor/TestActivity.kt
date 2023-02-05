package ru.yandex.yandexmaps.common.conductor

import android.app.Activity
import android.os.Bundle
import android.widget.FrameLayout
import com.bluelinelabs.conductor.Conductor
import com.bluelinelabs.conductor.Router

const val ROOT_ID = 42

open class TestActivity : Activity() {

    var bundle: Bundle? = null

    lateinit var router: Router

    override fun onCreate(savedInstanceState: Bundle?) {
        bundle = savedInstanceState
        super.onCreate(savedInstanceState)

        val root = FrameLayout(this)
        root.id = ROOT_ID
        setContentView(root)

        router = Conductor.attachRouter(this, this.findViewById(ROOT_ID), savedInstanceState)
    }
}
