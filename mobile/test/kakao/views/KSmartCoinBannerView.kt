package ru.yandex.market.test.kakao.views

import android.view.View
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.text.KButton
import io.github.kakaocup.kakao.text.KTextView
import org.hamcrest.Matcher
import ru.beru.android.R
import ru.yandex.market.test.kakao.assertions.SmartCoinBannerViewAssertions

class KSmartCoinBannerView : KBaseView<KSmartCoinBannerView>, SmartCoinBannerViewAssertions {

    private val function: ViewBuilder.() -> Unit
    private val parent: Matcher<View>?

    constructor(function: ViewBuilder.() -> Unit) : super(function) {
        this@KSmartCoinBannerView.function = function
        parent = null
    }

    constructor(parent: Matcher<View>, function: ViewBuilder.() -> Unit) : super(parent, function) {
        this@KSmartCoinBannerView.function = function
        this@KSmartCoinBannerView.parent = parent

    }

    override val actionButtonView: KButton
        get() {
            val childFunction: ViewBuilder.() -> Unit = {
                withId(R.id.actionButton)
                isDescendantOfA(this@KSmartCoinBannerView.function)
            }
            return if (parent != null) {
                KButton(parent, childFunction)
            } else {
                KButton(childFunction)
            }
        }

    override val titleView: KTextView
        get() {
            val childFunction: ViewBuilder.() -> Unit = {
                withId(R.id.titleTextView)
                isDescendantOfA(this@KSmartCoinBannerView.function)
            }
            return if (parent != null) {
                KTextView(parent, childFunction)
            } else {
                KTextView(childFunction)
            }
        }

    override val subtitleView: KTextView
        get() {
            val childFunction: ViewBuilder.() -> Unit = {
                withId(R.id.subtitleTextView)
                isDescendantOfA(this@KSmartCoinBannerView.function)
            }
            return if (parent != null) {
                KTextView(parent, childFunction)
            } else {
                KTextView(childFunction)
            }
        }

}