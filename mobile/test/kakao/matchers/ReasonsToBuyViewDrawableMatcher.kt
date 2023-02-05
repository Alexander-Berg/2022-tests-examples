package ru.yandex.market.test.kakao.matchers

import android.graphics.PorterDuff
import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import ru.yandex.market.utils.children
import ru.yandex.market.utils.getColorCompat
import ru.yandex.market.utils.tinted
import ru.yandex.market.utils.toBitmap
import ru.yandex.market.clean.presentation.feature.sku.ReasonsToBuyView
import ru.yandex.market.clean.presentation.view.NameplateView

class ReasonsToBuyViewDrawableMatcher(
    @DrawableRes private val id: Int,
    @ColorRes private val tint: Int? = null,
    private val tintMode: PorterDuff.Mode? = null
) : TypeSafeMatcher<View>() {

    override fun describeTo(description: Description) {
        description.appendText("ReasonToBuyView with drawable same as drawable with id $id")
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
        val expectedBitmap =
            drawable.tinted(tintColor, tintMode ?: PorterDuff.Mode.SRC_IN).toBitmap()
        return (view is ReasonsToBuyView) && view.children.any {
            it is NameplateView && it.icon?.toBitmap()?.sameAs(expectedBitmap) ?: false
        }
    }
}