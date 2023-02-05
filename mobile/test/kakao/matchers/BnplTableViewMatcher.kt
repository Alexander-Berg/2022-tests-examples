package ru.yandex.market.test.kakao.matchers

import android.view.View
import androidx.annotation.IdRes
import androidx.appcompat.widget.LinearLayoutCompat
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import ru.yandex.market.feature.bnpl.ui.BnplPaymentView
import ru.yandex.market.feature.bnpl.ui.BnplPaymentsTableView
import ru.yandex.market.utils.children
import ru.yandex.market.utils.isVisible

class BnplTableViewMatcher(
    @IdRes private val id: Int,
    @IdRes private val containerId: Int,
    @IdRes private val iconId: Int,
    private val paymentsCount: Int,
) : TypeSafeMatcher<View>() {

    override fun describeTo(description: Description) {
        description.appendText("BnplTableView with id $id paymentsCount: $paymentsCount")
    }

    override fun matchesSafely(item: View?): Boolean {
        if (item == null || item !is BnplPaymentsTableView) {
            return false
        }

        val container = item.findViewById<LinearLayoutCompat>(containerId)
        return container.childCount == paymentsCount &&
            (container.children.all { paymentView -> matchItem(paymentView as? BnplPaymentView) })
    }

    private fun matchItem(item: BnplPaymentView?): Boolean {
        return item != null &&
            item.isVisible &&
            item.findViewById<View>(iconId).isVisible
    }
}
