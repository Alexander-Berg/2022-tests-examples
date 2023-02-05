package ru.yandex.market.test.kakao.views

import android.view.View
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.text.KTextView
import org.hamcrest.Matcher
import ru.beru.android.R
import ru.yandex.market.uikit.view.PrescriptionBadgeView
import ru.yandex.market.test.kakao.util.isVisible

class KPrescriptionBadgeView(
    parent: Matcher<View>,
    function: ViewBuilder.() -> Unit
) : KBaseCompoundView<KPrescriptionBadgeView>(
    PrescriptionBadgeView::class,
    parent,
    function
) {

    val badge = KTextView {
        isDescendantOfA { withMatcher(this@KPrescriptionBadgeView.parentMatcher) }
        withId(R.id.badge)
    }

    fun checkBadgeVisibility(isVisible: Boolean) {
        badge.isVisible(isVisible = isVisible)
    }

}