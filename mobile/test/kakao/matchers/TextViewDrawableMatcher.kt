package ru.yandex.market.test.kakao.matchers

import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import ru.yandex.market.utils.compoundDrawableBottom
import ru.yandex.market.utils.compoundDrawableLeft
import ru.yandex.market.utils.compoundDrawableRight
import ru.yandex.market.utils.compoundDrawableTop
import ru.yandex.market.utils.getColorCompat
import ru.yandex.market.utils.tinted
import ru.yandex.market.utils.toBitmap
import ru.yandex.market.exception.NotFoundException


class TextViewDrawableMatcher(
    @DrawableRes private val id: Int,
    private val gravity: Int,
    @ColorRes private val tint: Int? = null,
    private val tintMode: PorterDuff.Mode? = null
) : TypeSafeMatcher<View>() {

    override fun describeTo(description: Description) {
        description.appendText("TextView with drawable same as drawable with id $id")
        tint?.let { description.appendText(", tint color id: $tint, mode: $tintMode") }
    }

    override fun matchesSafely(textView: View): Boolean {
        if (textView !is TextView) return false

        val context = textView.context
        val tintColor = if (tint != null) {
            context.getColorCompat(tint)
        } else {
            null
        }
        val drawable = AppCompatResources.getDrawable(context, id) ?: throw NotFoundException("Unable to find drawable")
        val expectedBitmap =
            drawable.tinted(tintColor, tintMode ?: PorterDuff.Mode.SRC_IN).toBitmap()

        val textViewDrawable = when (gravity) {
            Gravity.LEFT -> textView.compoundDrawableLeft
            Gravity.RIGHT -> textView.compoundDrawableRight
            Gravity.BOTTOM -> textView.compoundDrawableBottom
            else -> textView.compoundDrawableTop
        }

        return textViewDrawable is Drawable && textViewDrawable.toBitmap().sameAs(expectedBitmap)
    }

}