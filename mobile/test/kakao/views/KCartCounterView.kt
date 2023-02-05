package ru.yandex.market.test.kakao.views

import android.view.View
import androidx.annotation.ColorRes
import androidx.test.espresso.ViewAssertion
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.image.KImageView
import io.github.kakaocup.kakao.text.KButton
import org.hamcrest.Matcher
import ru.beru.android.R
import ru.yandex.market.test.kakao.matchers.DrawableMatcher
import ru.yandex.market.test.util.hasAlpha
import ru.yandex.market.feature.cartbutton.ui.CartButton
import ru.yandex.market.feature.cartbutton.CartButtonState.ButtonState
import ru.yandex.market.test.kakao.util.isEnabled
import ru.yandex.market.test.kakao.util.isVisibleAndEnabled
import ru.yandex.market.utils.OPAQUE
import ru.yandex.market.utils.exhaustive

class KCartCounterView : KBaseCompoundView<KCartCounterView> {

    private val cartButtonProgressButton = KButton(parentMatcher) {
        withId(R.id.cartButtonProgressButton)
    }

    private val cartPlusButton = KImageView(parentMatcher) {
        withId(R.id.cartPlusButton)
    }

    private val cartMinusButton = KImageView(parentMatcher) {
        withId(R.id.cartMinusButton)
    }

    constructor(function: ViewBuilder.() -> Unit) : super(CartButton::class, function)

    constructor(parent: Matcher<View>, function: ViewBuilder.() -> Unit) : super(CartButton::class, parent, function)

    fun checkButtonDisplayState(state: ButtonState, style: CartCounterButtonStyle, text: String) {
        checkState(state, text)
        checkStyle(style)
    }

    fun checkCartButtonState(state: ButtonState, text: String) {
        checkState(state, text)
    }

    private fun checkStyle(style: CartCounterButtonStyle) {
        when (style) {
            CartCounterButtonStyle.OUTLINED_LARGE -> {
                cartButtonMatches {
                    withMatcher(DrawableMatcher(R.drawable.bg_button_outlined_large, null, null))
                }
                hasTextColor(R.color.text_black)
            }
            CartCounterButtonStyle.FILLED_LARGE -> {
                cartButtonMatches {
                    withMatcher(DrawableMatcher(R.drawable.bg_button_filled_large, null, null))
                }
                hasTextColor(R.color.text_black)
            }
            CartCounterButtonStyle.OUTLINED_SMALL -> {
                cartButtonMatches {
                    withMatcher(DrawableMatcher(R.drawable.bg_button_outlined_small, null, null))
                }
                hasTextColor(R.color.text_black)
            }
            CartCounterButtonStyle.FILLED_SMALL -> {
                cartButtonMatches {
                    withMatcher(DrawableMatcher(R.drawable.bg_button_filled_small, null, null))
                }
                hasTextColor(R.color.text_black)
            }
            CartCounterButtonStyle.WEAKLY_FILLED -> {
                cartButtonMatches {
                    withMatcher(DrawableMatcher(R.drawable.bg_button_weakly_filled, null, null))
                }
                hasTextColor(R.color.text_black)
            }
            CartCounterButtonStyle.OUTLINED_MEDIUM_REDESIGN -> {
                cartButtonMatches {
                    withMatcher(DrawableMatcher(R.drawable.bg_button_outlined_medium_redesign, null, null))
                }
                hasTextColor(R.color.text_black)
            }
            CartCounterButtonStyle.FILLED_MEDIUM_REDESIGN -> {
                cartButtonMatches {
                    withMatcher(DrawableMatcher(R.drawable.bg_button_filled_medium_redesign, null, null))
                }
                hasTextColor(R.color.text_black)
            }
            CartCounterButtonStyle.ANY -> {
                // no-op
            }
        }
    }

    fun cartButtonMatches(function: ViewBuilder.() -> Unit) {
        cartButtonProgressButton.matches(function)
    }

    fun hasText(text: String) {
        cartButtonProgressButton.hasText(text)
    }

    fun hasAnyText() {
        cartButtonProgressButton.hasAnyText()
    }

    fun hasTextColor(@ColorRes resId: Int) {
        cartButtonProgressButton.hasTextColor(resId = resId)
    }

    fun clickPlusButton() {
        cartPlusButton.click()
    }

    fun isPlusButtonEnabled(isEnabled: Boolean) {
        cartPlusButton.isEnabled(isEnabled = isEnabled)
    }

    fun isMinusButtonEnabled(isEnabled: Boolean) {
        cartMinusButton.isEnabled(isEnabled = isEnabled)
    }

    fun isPlusButtonVisibleAndEnabled(isVisible: Boolean = true, isEnabled: Boolean = true) {
        cartPlusButton.isVisibleAndEnabled(isVisible, isEnabled)
    }

    fun isMinusButtonVisibleAndEnabled(isVisible: Boolean = true, isEnabled: Boolean = true) {
        cartMinusButton.isVisibleAndEnabled(isVisible, isEnabled)
    }

    fun clickMinusButton() {
        cartMinusButton.click()
    }

    private fun checkState(state: ButtonState, text: String) {
        when (state) {
            ButtonState.LOCKED_TO_REMOVE,
            ButtonState.NOT_IN_CART,
            ButtonState.IN_CART -> {
                hasText(text)
            }

            ButtonState.PREORDER -> {
                isProgressVisible(false)
                isEnabled()
                hasAlpha(OPAQUE)
                hasText(text)
            }

            ButtonState.PROGRESS -> {
                isProgressVisible(true)
                hasAnyText()
            }

            ButtonState.NOT_FOR_SALE,
            ButtonState.OUT_OF_STOCK -> {
                isProgressVisible(false)
                isDisabled()
                hasAlpha(0.2f)
            }
        }.exhaustive
    }

    private fun isProgressVisible(visible: Boolean) {
        view.check(
            ViewAssertion { view, noViewFoundException ->
                if (view is CartButton) {
                    val actualVisible = view.isProgressVisible()
                    if (visible != actualVisible) {
                        throw AssertionError("Wrong progress state! Expected $visible, but actual is $actualVisible")
                    }
                } else {
                    noViewFoundException.let {
                        throw AssertionError(it)
                    }
                }
            }
        )
    }

    enum class CartCounterButtonStyle {
        OUTLINED_LARGE,
        FILLED_LARGE,
        OUTLINED_SMALL,
        FILLED_SMALL,
        WEAKLY_FILLED,
        OUTLINED_MEDIUM_REDESIGN,
        FILLED_MEDIUM_REDESIGN,
        ANY,
    }
}
