package ru.yandex.market.test.kakao.matchers

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import ru.yandex.market.screen.checkout.confirm.item.PaymentMethodItem
import ru.yandex.market.ui.view.CashbackBadgeView
import ru.yandex.market.utils.children

class PaymentOptionsBadgeDrawablesMatcher(
    private val expectedBadges: List<PaymentMethodItem.Badge>
) : TypeSafeMatcher<View>() {

    override fun describeTo(description: Description) {
        description.appendText("Payment option has badges drawables with ids $expectedBadges")
    }

    override fun matchesSafely(view: View): Boolean {
        if (view !is ViewGroup) return false
        if (view.childCount != expectedBadges.size) return false

        val context = view.context
        var viewMatches = true

        view.children.forEachIndexed { i, child ->
            when (val badge = expectedBadges[i]) {
                is PaymentMethodItem.Badge.Icon -> {
                    val drawable = AppCompatResources.getDrawable(context, badge.icon)
                    viewMatches = viewMatches && (child as? ImageView)?.drawable.matches(drawable)
                }
                is PaymentMethodItem.Badge.Plus -> {
                    viewMatches = child is CashbackBadgeView
                    if (viewMatches && badge.text != null) {
                        viewMatches = ViewMatchers.hasDescendant(ViewMatchers.withText(badge.text)).matches(child)
                    }
                }
            }

        }

        return viewMatches
    }
}