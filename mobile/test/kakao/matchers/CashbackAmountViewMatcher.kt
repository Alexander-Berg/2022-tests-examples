package ru.yandex.market.test.kakao.matchers

import android.view.View
import com.yandex.plus.home.badge.widget.CashbackAmountView
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher

class CashbackAmountViewMatcher(private val cashbackTextValue: String) : TypeSafeMatcher<View>() {

    override fun describeTo(description: Description?) {
        description?.appendText("CashbackAmountView has text")
    }

    override fun matchesSafely(item: View?): Boolean {
        return item is CashbackAmountView && matchCashbackValue(item)

    }

    //Без рефлексии тут не обойтись, т.к вьюха из библиотеки и у неё нет публичных методов для получения значений
    private fun matchCashbackValue(cashbackAmountView: CashbackAmountView): Boolean {
        return try {
            val cashbackValueField =
                CashbackAmountView::class.java.getDeclaredField("currentText")
            cashbackValueField.isAccessible = true
            cashbackTextValue == cashbackValueField.get(cashbackAmountView)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}