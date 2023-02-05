package ru.yandex.market.test.kakao.views

import androidx.core.view.isVisible
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import kotlinx.android.synthetic.main.view_story_slide.view.skuView
import kotlinx.android.synthetic.main.view_story_slide.view.storySlideButton
import kotlinx.android.synthetic.main.view_story_slide.view.storySlideImageBackground
import kotlinx.android.synthetic.main.view_story_slide.view.storySlideSubtitle
import kotlinx.android.synthetic.main.view_story_slide.view.storySlideTitle
import ru.yandex.market.clean.presentation.feature.stories.views.StorySlideView
import ru.yandex.market.test.util.assertThat
import ru.yandex.market.test.util.createErrorMessage
import ru.yandex.market.test.util.withView

class KStorySlideView(function: ViewBuilder.() -> Unit) : KBaseView<KStorySlideView>(function) {

    fun checkTitle(text: String) {
        withView<StorySlideView> { slide ->
            assertThat(slide.storySlideTitle.text == text) {
                createErrorMessage("subtitle", text, slide.storySlideTitle.text)
            }
        }
    }

    fun checkSubtitle(text: String) {
        withView<StorySlideView> { slide ->
            assertThat(slide.storySlideSubtitle.text == text) {
                createErrorMessage("subtitle", text, slide.storySlideSubtitle.text)
            }
        }
    }

    fun clickOnSlideButton() {
        withView<StorySlideView> { slide ->
            assertThat(slide.storySlideButton.callOnClick()) {
                createErrorMessage("failed to click storySlideButton")
            }
        }
    }

    fun isBackgroundVisible(isVisible: Boolean) {
        withView<StorySlideView> { slide ->
            assertThat(slide.storySlideImageBackground.isVisible != isVisible) {
                createErrorMessage("background", isVisible, slide.storySlideImageBackground.isVisible)
            }
        }
    }

    fun checkSkuIsVisible(isVisible: Boolean) {
        withView<StorySlideView> { slide ->
            assertThat(slide.skuView.isVisible == isVisible) {
                createErrorMessage("SkuView", isVisible, slide.skuView.isVisible)
            }
        }
    }
}