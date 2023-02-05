package ru.yandex.market.test.kakao.matchers

import android.view.View
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.appcompat.widget.LinearLayoutCompat
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import ru.yandex.market.feature.bnpl.ui.BnplPaymentsTableView

class BnplTableViewActiveMatcher(
    private val isActive: Boolean,
    @IdRes private val containerId: Int,
    @IdRes private val amountTextViewId: Int,
    @IdRes private val dateTextViewId: Int,
) : TypeSafeMatcher<View>() {

    override fun describeTo(description: Description) {
        val result = if (isActive) {
            "active"
        } else {
            "not active"
        }
        description.appendText("BnplTableView graphic is ${result})")
    }

    override fun matchesSafely(item: View): Boolean {
        if (item == null || item !is BnplPaymentsTableView) {
            return false
        }
        val container = item.findViewById<LinearLayoutCompat>(containerId)
        val firstPayment = container.getChildAt(0) ?: return false
        val alpha = if (isActive) {
            ACTIVE_ALPHA
        } else {
            INACTIVE_ALPHA
        }
        return firstPayment.findViewById<TextView>(amountTextViewId).alpha == alpha &&
            firstPayment.findViewById<TextView>(dateTextViewId).alpha == alpha
    }

    companion object {
        private const val INACTIVE_ALPHA = 0.35f
        private const val ACTIVE_ALPHA = 1f
    }
}
