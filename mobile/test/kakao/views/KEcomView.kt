package ru.yandex.market.test.kakao.views

import android.view.View
import androidx.test.espresso.matcher.ViewMatchers
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.common.views.KView
import io.github.kakaocup.kakao.image.KImageView
import io.github.kakaocup.kakao.text.KButton
import io.github.kakaocup.kakao.text.KTextView
import org.hamcrest.Matcher
import ru.beru.android.R
import ru.yandex.market.test.kakao.util.withPosition

class KEcomView(parent: Matcher<View>, function: ViewBuilder.() -> Unit) : KBaseView<KEcomView>(parent, function) {

    private val title = KTextView(parent) {
        withId(R.id.titleView)
    }

    private val subTitle = KTextView(parent) {
        withId(R.id.subtitleView)
    }

    private val hideQuestion = KImageView(parent) {
        withId(R.id.hideQuestion)
    }

    private val image = KImageView(parent) {
        withId(R.id.questionImageView)
    }

    private val options = KView(parent) {
        withId(R.id.optionsContainer)
    }

    private val button = KButton(parent) {
        withId(R.id.nextStepView)
    }

    fun clickByOption(position: Int) {
        val optionView = KView {
            withParent { withId(R.id.optionsContainer) }
            withPosition(position)
        }
        optionView.click()
    }

    fun doubleClickByOption(position: Int) {
        val optionView = KView {
            withParent { withId(R.id.optionsContainer) }
            withPosition(position)
        }
        optionView.doubleClick()
    }

    fun clickReady() {
        button.click()
    }

    fun clickHide() {
        hideQuestion.click()
    }

    fun checkVisibleTitle() {
        title.isVisible()
    }

    fun checkTextTitle(text: String) {
        title.hasText(text)
    }

    fun checkVisibleSubTitle() {
        subTitle.isVisible()
    }

    fun checkTextSubTitle(text: String) {
        subTitle.hasText(text)
    }

    fun checkDisabledButtonReady() {
        button.isVisible()
        button.isDisabled()
    }

    fun checkEnabledButtonReady() {
        button.isVisible()
        button.isEnabled()
    }

    fun checkBackgroundOption(position: Int) {
        val optionView = KView {
            withParent { withId(R.id.optionsContainer) }
            withPosition(position)
        }
        optionView.hasBackground(R.drawable.bg_ecom_question_option_checked)
    }

    fun checkUncheckedBackgroundOption(position: Int) {
        val optionView = KView {
            withParent { withId(R.id.optionsContainer) }
            withPosition(position)
        }
        optionView.hasBackground(R.drawable.bg_ecom_question_option_normal)
    }

    fun checkVisibleHideQuestion() {
        hideQuestion.isVisible()
    }

    fun checkVisibleImage() {
        image.isVisible()
    }

    fun checkCountOptions(count: Int) {
        options.invoke {
            ViewMatchers.hasChildCount(count)
        }
    }

}
