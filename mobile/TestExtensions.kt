package com.yandex.mail

import android.graphics.Canvas
import android.view.View

/**
 * Prepare view to test clicks and other stats. It could be used to test click on adapter items.
 */
fun View.traverseLayout() {
    measure(320, 320)
    layout(0, 0, 320, 320)
    draw(Canvas())
}
