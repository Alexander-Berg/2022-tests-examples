package ru.yandex.market.test.kakao.views

import android.widget.ImageView
import androidx.test.espresso.ViewAssertion
import io.github.kakaocup.kakao.image.KImageView

fun KImageView.hasImage() {
    view.check(
        ViewAssertion { view, notFoundException ->
            if (view is ImageView) {
                view.apply {
                    isDrawingCacheEnabled = true
                    try {
                        drawingCache.apply {
                            for (y in 0 until height) {
                                for (x in 0 until width) {
                                    if (getPixel(x, y) != 0) {
                                        return@ViewAssertion
                                    }
                                }
                            }
                        }
                        throw AssertionError("ImageView doesn't have an image")
                    } finally {
                        destroyDrawingCache()
                    }
                }
            } else {
                notFoundException.let {
                    throw AssertionError(it)
                }
            }
        }
    )
}