package ru.yandex.market.test.kakao.views

import androidx.annotation.DrawableRes
import androidx.test.espresso.ViewAssertion
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import ru.yandex.market.utils.toBitmap
import ru.yandex.market.clean.presentation.view.NameplateView

class KNameplateView(function: ViewBuilder.() -> Unit) : KBaseView<KNameplateView>(function)

fun KNameplateView.checkTitle(title: String) {
    view.check(
        ViewAssertion { view, notFoundException ->
            if (view is NameplateView) {
                if (view.titleText.toString() == title) {
                    return@ViewAssertion
                }
                throw AssertionError("Expected $title title got ${view.titleText}")
            } else {
                notFoundException.let {
                    throw AssertionError(it)
                }
            }
        }
    )
}

fun KNameplateView.checkSubtitle(subtitle: String) {
    view.check(
        ViewAssertion { view, notFoundException ->
            if (view is NameplateView) {
                if (view.subtitleText.toString() == subtitle) {
                    return@ViewAssertion
                }
                throw AssertionError("Expected subtitle $subtitle got ${view.subtitleText}")
            } else {
                notFoundException.let {
                    throw AssertionError(it)
                }
            }
        }
    )
}

fun KNameplateView.checkIcon(@DrawableRes iconRes: Int) {
    view.check(
        ViewAssertion { view, notFoundException ->
            if (view is NameplateView) {
                val icon = view.icon?.toBitmap()
                val expectedIcon = view.context?.getDrawable(iconRes)?.toBitmap()
                if (icon?.sameAs(expectedIcon) ?: (expectedIcon == null)) {
                    return@ViewAssertion
                }
                throw AssertionError("Wrong NamePlate icon, expected $iconRes")
            } else {
                notFoundException.let {
                    throw AssertionError(it)
                }
            }
        }
    )
}

fun KNameplateView.checkBadge(@DrawableRes badgeRes: Int, isVisible: Boolean = true) {
    view.check(
        ViewAssertion { view, noViewFoundException ->
            if (view is NameplateView) {
                val badge = view.titleBadge?.toBitmap()
                val expectedBadge = view.context?.getDrawable(badgeRes)?.toBitmap()
                if (badge?.sameAs(expectedBadge) ?: (expectedBadge == null || !isVisible)) {
                    return@ViewAssertion
                }
                throw AssertionError("Wrong NamePlate badge, expected $badgeRes")
            } else {
                noViewFoundException.let {
                    throw AssertionError(it)
                }
            }
        }
    )
}
