package ru.yandex.market.test.kakao.views

import android.widget.LinearLayout
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.common.views.KView
import io.github.kakaocup.kakao.image.KImageView
import ru.beru.android.R
import ru.yandex.market.test.kakao.util.isNotVisible

class KAddVideoView(function: ViewBuilder.() -> Unit) : KBaseView<LinearLayout>(function) {

    private val viewEmpty = KView {
        withId(R.id.viewProductUserVideoEmpty)
        isDescendantOfA(function)
    }

    private val viewRemove = KImageView {
        withId(R.id.imageProductUserVideoRemove)
        isDescendantOfA(function)
    }

    private val viewVideo = KImageView {
        withId(R.id.imageProductUserVideo)
        isDescendantOfA(function)
    }

    fun clickAdd() {
        viewEmpty.click()
    }

    fun checkCantAdd() {
        viewEmpty.isNotVisible()
    }

    fun checkCanAdd() {
        viewEmpty.isVisible()
    }

    fun clickRemove() {
        viewRemove.click()
    }

    fun checkVideoAdded() {
        viewVideo.isVisible()
    }

    fun checkVideoNotAdded() {
        viewVideo.isNotVisible()
    }
}