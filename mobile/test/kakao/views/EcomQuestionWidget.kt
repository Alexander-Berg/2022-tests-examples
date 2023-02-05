package ru.yandex.market.test.kakao.views

import android.view.View
import io.github.kakaocup.kakao.recycler.KRecyclerItem
import org.hamcrest.Matcher
import ru.beru.android.R
import ru.yandex.market.screen.CmsScreen

class EcomQuestionWidget(parent: Matcher<View>) : KRecyclerItem<CmsScreen.ScrollBoxStoriesWidget>(parent) {
    private val ecomView = KEcomView(parent) {
        withId(R.id.ecomQuestionView)
    }

    fun clickByOption(position: Int) {
        ecomView.clickByOption(position)
    }

    fun doubleClickByOption(position: Int) {
        ecomView.doubleClickByOption(position)
    }

    fun clickReady() {
        ecomView.clickReady()
    }

    fun clickHide() {
        ecomView.clickHide()
    }

    fun checkTitleText(text: String) {
        ecomView.checkVisibleTitle()
        ecomView.checkTextTitle(text)
    }

    fun checkSubTitleText(text: String) {
        ecomView.checkVisibleSubTitle()
        ecomView.checkTextSubTitle(text)
    }

    fun checkCountOptions(count: Int) {
        ecomView.checkCountOptions(count)
    }

    fun checkDisabledButtonReady() {
        ecomView.checkDisabledButtonReady()
    }

    fun checkEnabledButtonReady() {
        ecomView.checkEnabledButtonReady()
    }

    fun checkUncheckedBackgroundOption(position: Int) {
        ecomView.checkUncheckedBackgroundOption(position)
    }

    fun checkBackgroundOption(position: Int) {
        ecomView.checkBackgroundOption(position)
    }

    fun checkVisibleHideQuestion() {
        ecomView.checkVisibleHideQuestion()
    }

    fun checkVisibleImage() {
        ecomView.checkVisibleImage()
    }

    fun checkIsNotDisplayed() {
        isNotDisplayed()
    }

    fun checkIsDisplayed() {
        isDisplayed()
    }
}
