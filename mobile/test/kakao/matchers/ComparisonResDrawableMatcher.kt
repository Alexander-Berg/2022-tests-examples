package ru.yandex.market.test.kakao.matchers

import android.graphics.PorterDuff
import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageView
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import ru.yandex.market.utils.getColorCompat
import ru.yandex.market.utils.isVisible
import ru.yandex.market.utils.tinted
import ru.yandex.market.utils.toBitmap

class ComparisonResDrawableMatcher(
    @DrawableRes private val id: Int,
    @ColorRes private val tint: Int? = null,
    private val tintMode: PorterDuff.Mode? = null
) : TypeSafeMatcher<View>() {

    override fun describeTo(description: Description) {
        description.appendText("Comparison item $id visible and comparison")
    }

    override fun matchesSafely(view: View): Boolean {
        val context = view.context
        val tintColor = if (tint != null) {
            context.getColorCompat(tint)
        } else {
            null
        }
        val drawable = context.getDrawable(id) ?: return false
        val expectedBitmap =
            drawable.tinted(tintColor, tintMode ?: PorterDuff.Mode.SRC_IN).toBitmap()
        return view.isVisible && view is AppCompatImageView &&
                view.drawable.toBitmap().sameAs(expectedBitmap)
    }
}