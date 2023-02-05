package ru.yandex.market.test.kakao.assertions

import io.github.kakaocup.kakao.text.KButton
import io.github.kakaocup.kakao.text.KTextView

interface SmartCoinBannerViewAssertions {

    val actionButtonView: KButton

    val titleView: KTextView

    val subtitleView: KTextView

    fun hasButton(buttonLabel: String) {
        actionButtonView.isVisible()
        actionButtonView.hasText(buttonLabel)
    }

    fun hasTitle(title: String) {
        titleView.isVisible()
        titleView.hasText(title)
    }

    fun hasSubtitle(subtitle: String?) {
        if (subtitle != null) {
            subtitleView.isVisible()
            subtitleView.hasText(subtitle)
        } else {
            subtitleView.isGone()
        }
    }

}