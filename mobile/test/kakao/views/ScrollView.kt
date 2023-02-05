package ru.yandex.market.test.kakao.views

import io.github.kakaocup.kakao.scroll.KScrollView
import ru.yandex.market.test.kakao.util.scrolledToEnd
import ru.yandex.market.test.kakao.util.scrolledToStart

fun KScrollView.scrolledAtStart() {
    matches { scrolledToStart() }
}

fun KScrollView.scrolledAtEnd() {
    matches { scrolledToEnd() }
}