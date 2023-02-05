package ru.yandex.market.test.kakao.gridlayout

import android.view.View
import androidx.test.espresso.assertion.ViewAssertions
import io.github.kakaocup.kakao.common.assertions.BaseAssertions
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import ru.yandex.market.test.kakao.matchers.GridLayoutRowsMatcher

interface GridLayoutAssertions : BaseAssertions {

    fun hasRow(matcher: Matcher<Iterable<View>>) {
        view.check(ViewAssertions.matches(GridLayoutRowsMatcher(Matchers.contains(matcher))))
    }

    fun hasRows(matchers: List<Matcher<Iterable<View>>>) {
        view.check(ViewAssertions.matches(GridLayoutRowsMatcher(Matchers.contains(matchers))))
    }

    fun hasRowsInAnyOrder(matchers: Collection<Matcher<Iterable<View>>>) {
        view.check(ViewAssertions.matches(GridLayoutRowsMatcher(Matchers.containsInAnyOrder(matchers))))
    }
}