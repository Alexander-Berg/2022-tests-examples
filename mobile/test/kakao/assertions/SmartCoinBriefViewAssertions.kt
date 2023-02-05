package ru.yandex.market.test.kakao.assertions

import io.github.kakaocup.kakao.image.KImageView
import io.github.kakaocup.kakao.text.KTextView

interface SmartCoinBriefViewAssertions {

    val smartCoinBriefImage: KImageView

    val smartCoinBriefTitle: KTextView

    val smartCoinBriefSubtitle: KTextView

    fun hasImage() {
        smartCoinBriefImage.isVisible()
    }

    fun hasTitle(title: String, isActive: Boolean) {
        smartCoinBriefTitle.isVisible()
        if (isActive) {
            smartCoinBriefTitle.hasText(title)
        } else {
            smartCoinBriefTitle.containsText(title)
        }
    }

    fun hasSubtitle(subtitle: String?) {
        if (subtitle != null) {
            smartCoinBriefSubtitle.isVisible()
            smartCoinBriefSubtitle.hasText(subtitle)
        } else {
            smartCoinBriefSubtitle.isGone()
        }
    }

}