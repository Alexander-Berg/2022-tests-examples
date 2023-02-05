package ru.yandex.market.test.kakao.matchers

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher

class CompoundDrawableMatcher(private val drawableRes: Int?, private val drawablePosition: DrawablePosition) :
    TypeSafeMatcher<View>() {

    override fun describeTo(description: Description?) {
        description?.appendText("Text view has ${drawablePosition.decription} with resource $drawableRes")
    }

    override fun matchesSafely(item: View?): Boolean {
        return (item as? TextView)?.let { textView ->
            var expectedDrawable: Drawable? =
                drawableRes?.let { drawable -> ContextCompat.getDrawable(textView.context, drawable) }

            val actualDrawable = textView.compoundDrawables[drawablePosition.position]

            if (expectedDrawable != null) {
                val actualBitmap = drawableToBitmap(actualDrawable)
                val expectedBitmap = drawableToBitmap(expectedDrawable)

                return actualBitmap.sameAs(expectedBitmap)
            }
            return actualDrawable == null
        } ?: false
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            if (drawable.bitmap != null) {
                return drawable.bitmap
            }
        }

        if (drawable is StateListDrawable) {
            if (drawable.getCurrent() is BitmapDrawable) {
                val bitmapDrawable = drawable.getCurrent() as BitmapDrawable
                if (bitmapDrawable.bitmap != null) {
                    return bitmapDrawable.bitmap
                }
            }
        }

        val bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888) // Single color bitmap will be created of 1x1 pixel
        } else {
            Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        }

        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    enum class DrawablePosition(val position: Int, val decription: String) {
        LEFT(0, "DrawableLeft"),
        TOP(1, "DrawableTop"),
        RIGHT(2, "DrawableRight"),
        BOTTOM(3, "DrawableBottom")
    }
}