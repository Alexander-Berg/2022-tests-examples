package ru.yandex.market.test.kakao.views

import android.view.View
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.image.KImageView
import io.github.kakaocup.kakao.text.KTextView
import org.hamcrest.Matcher
import ru.beru.android.R
import ru.yandex.market.test.kakao.assertions.SmartCoinBriefViewAssertions

class KSmartCoinBriefView : KBaseView<KSmartCoinBriefView>, SmartCoinBriefViewAssertions {

    private val function: ViewBuilder.() -> Unit
    private val parent: Matcher<View>?

    constructor(function: ViewBuilder.() -> Unit) : super(function) {
        this@KSmartCoinBriefView.function = function
        parent = null
    }

    constructor(parent: Matcher<View>, function: ViewBuilder.() -> Unit) : super(parent, function) {
        this@KSmartCoinBriefView.function = function
        this@KSmartCoinBriefView.parent = parent

    }

    override val smartCoinBriefImage: KImageView
        get() {
            val childFunction: ViewBuilder.() -> Unit = {
                withId(R.id.smartCoinBriefImage)
                isDescendantOfA(this@KSmartCoinBriefView.function)
            }
            return if (parent != null) {
                KImageView(parent, childFunction)
            } else {
                KImageView(childFunction)
            }
        }

    override val smartCoinBriefTitle: KTextView
        get() {
            val childFunction: ViewBuilder.() -> Unit = {
                withId(R.id.smartCoinBriefTitle)
                isDescendantOfA(this@KSmartCoinBriefView.function)
            }
            return if (parent != null) {
                KTextView(parent, childFunction)
            } else {
                KTextView(childFunction)
            }
        }

    override val smartCoinBriefSubtitle: KTextView
        get() {
            val childFunction: ViewBuilder.() -> Unit = {
                withId(R.id.smartCoinBriefSubtitle)
                isDescendantOfA(this@KSmartCoinBriefView.function)
            }
            return if (parent != null) {
                KTextView(parent, childFunction)
            } else {
                KTextView(childFunction)
            }
        }

}