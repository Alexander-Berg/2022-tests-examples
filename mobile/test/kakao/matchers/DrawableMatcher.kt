package ru.yandex.market.test.kakao.matchers

import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.graphics.drawable.VectorDrawable
import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import ru.yandex.market.drawables.SizedDrawableWrapper
import ru.yandex.market.utils.getColorCompat
import ru.yandex.market.utils.tinted
import ru.yandex.market.utils.toBitmap

class DrawableMatcher(
    @DrawableRes private val id: Int,
    @ColorRes private val tint: Int? = null,
    private val tintMode: PorterDuff.Mode? = null
) : TypeSafeMatcher<View>() {

    override fun describeTo(description: Description) {
        description.appendText("ImageView with drawable same as drawable with id $id")
        tint?.let { description.appendText(", tint color id: $tint, mode: $tintMode") }
    }

    override fun matchesSafely(view: View): Boolean {
        val context = view.context
        val tintColor = if (tint != null) {
            context.getColorCompat(tint)
        } else {
            null
        }

        val drawable = context.getDrawable(id) ?: return false
        drawable.tinted(tintColor, tintMode ?: PorterDuff.Mode.SRC_IN)

        return view.background.matches(drawable)
    }
}

fun Drawable?.matches(other: Drawable?): Boolean {
    var actualDrawable = this
    var expectedDrawable = other

    if (actualDrawable == null || expectedDrawable == null) {
        return false
    }

    if (actualDrawable is StateListDrawable && expectedDrawable is StateListDrawable) {
        expectedDrawable.setState(actualDrawable.state)
        expectedDrawable.setLevel(actualDrawable.level)
        expectedDrawable.setExitFadeDuration(1)
        expectedDrawable.setEnterFadeDuration(1)
        expectedDrawable = expectedDrawable.current
        expectedDrawable.alpha = 255
        actualDrawable = actualDrawable.current
    }

    if (actualDrawable is BitmapDrawable) {
        val bitmap = actualDrawable.bitmap
        val otherBitmap = (expectedDrawable as BitmapDrawable).bitmap
        return bitmap.sameAs(otherBitmap)
    }

    if (actualDrawable is VectorDrawable ||
        actualDrawable is VectorDrawableCompat ||
        actualDrawable is GradientDrawable ||
        actualDrawable is SizedDrawableWrapper
    ) {
        val drawableRect = actualDrawable.bounds
        val actualBitmap = actualDrawable.toBitmap(drawableRect.width(), drawableRect.height(), null)
        val expectedBitmap = expectedDrawable.toBitmap(drawableRect.width(), drawableRect.height(), null)
        return actualBitmap.sameAs(expectedBitmap)
    }

    return false
}