package ru.yandex.market.test.kakao.matchers

import android.graphics.PorterDuff
import android.view.View
import androidx.annotation.ColorRes
import androidx.appcompat.widget.AppCompatImageView
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import ru.beru.android.R
import ru.yandex.market.utils.getColorCompat
import ru.yandex.market.utils.isVisible
import ru.yandex.market.utils.tinted
import ru.yandex.market.utils.toBitmap

class SearchItemWishListDrawableMatcher(
    private val isSelected: Boolean,
    @ColorRes private val tint: Int? = null,
    private val tintMode: PorterDuff.Mode? = null
) : TypeSafeMatcher<View>() {

    override fun describeTo(description: Description) {
        description.appendText("Wish item visible and " + (if (isSelected) "" else "not ") + "selected")
    }

    override fun matchesSafely(view: View): Boolean {
        val id = if (isSelected) R.drawable.ic_heart_filled_16 else R.drawable.ic_heart_white_16
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