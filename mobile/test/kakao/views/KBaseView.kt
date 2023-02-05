package ru.yandex.market.test.kakao.views

import android.graphics.PorterDuff
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.test.espresso.assertion.ViewAssertions
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.recycler.KRecyclerItem
import io.github.kakaocup.kakao.recycler.KRecyclerView
import ru.yandex.market.test.kakao.actions.NestedScrollTo
import ru.yandex.market.test.kakao.matchers.DrawableMatcher

fun KBaseView<*>.nestedScrollTo() {
    act { NestedScrollTo() }
}

fun KRecyclerItem<*>.nestedScrollTo() {
    act { NestedScrollTo() }
}

fun KRecyclerView.nestedScrollTo() {
    act { NestedScrollTo() }
}

fun KBaseView<*>.hasBackground(@DrawableRes id: Int, @ColorRes tint: Int? = null, tintMode: PorterDuff.Mode? = null) {
    view.check(ViewAssertions.matches(DrawableMatcher(id, tint, tintMode)))
}