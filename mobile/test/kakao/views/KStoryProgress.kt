package ru.yandex.market.test.kakao.views

import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import ru.yandex.market.clean.presentation.feature.stories.views.StoriesSlideIndicator
import ru.yandex.market.test.util.assertThat
import ru.yandex.market.test.util.createErrorMessage
import ru.yandex.market.test.util.withView

class KStoryProgress(function: ViewBuilder.() -> Unit) : KBaseView<KStorySlideView>(function) {

    fun checkProgressIsRunning() {
        withView<StoriesSlideIndicator> { indicator ->
            assertThat(indicator.currentPageProgress > 0) {
                createErrorMessage(
                    element = "Indicator progress",
                    actual = "page = ${indicator.currentPage}, progress = ${indicator.currentPageProgress}"
                )
            }
        }
    }

    fun checkSlide(slidePosition: Int) {
        withView<StoriesSlideIndicator> { indicator ->
            assertThat(indicator.currentPage == slidePosition) {
                createErrorMessage(
                    element = "Indicator page",
                    actual = "${indicator.currentPage}",
                    expected = slidePosition
                )
            }
        }
    }
}