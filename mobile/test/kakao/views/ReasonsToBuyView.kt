package ru.yandex.market.test.kakao.views

import androidx.annotation.DrawableRes
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import ru.beru.android.R
import ru.yandex.market.clean.presentation.feature.sku.ReasonsToBuyView
import ru.yandex.market.test.kakao.matchers.ReasonsToBuyViewDrawableMatcher
import ru.yandex.market.test.kakao.matchers.ReasonsToBuyViewTextMatcher

class KReasonsToBuyView(function: ViewBuilder.() -> Unit) : KBaseView<ReasonsToBuyView>(function) {

    fun hasDrawable(@DrawableRes resId: Int) {
        matches { withMatcher(ReasonsToBuyViewDrawableMatcher(resId)) }
    }

    fun hasTitle(text: String) {
        matches { withMatcher(ReasonsToBuyViewTextMatcher(R.id.nameplateTitleView, text)) }
    }

    fun hasDescription(text: String) {
        matches {
            withMatcher(ReasonsToBuyViewTextMatcher(R.id.nameplateSubtitleView, text))
        }
    }
}