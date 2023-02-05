package ru.yandex.market.test.kakao.views

import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.common.views.KView
import io.github.kakaocup.kakao.image.KImageView
import ru.beru.android.R
import ru.yandex.market.test.kakao.matchers.MinimumAlphaMatcher
import ru.yandex.market.test.kakao.util.isNotVisible
import ru.yandex.market.test.util.hasAlpha

class KVideoView(function: ViewBuilder.() -> Unit) : KBaseView<KPhotoView>(function) {
    private val videoMute = KImageView {
        withId(R.id.mute)
        withParent(function)
    }

    private val videoFullscreen = KView {
        withId(R.id.fullscreen)
        withParent(function)
    }

    private val videoTimer = KView {
        withId(R.id.timer)
        withParent(function)
    }

    private val videoPlay = KView {
        withId(R.id.play)
        withParent(function)
    }

    private val videoPause = KView {
        withId(R.id.pause)
        withParent(function)
    }

    fun clickVideoPlay() {
        videoPlay.click()
    }

    fun clickVideoPause() {
        videoPause.click()
    }

    fun clickVideoMute() {
        videoMute.click()
    }

    fun clickVideoFullscreen() {
        videoFullscreen.click()
    }

    fun checkPlayVisible() {
        videoPlay.isVisible()
    }

    fun checkPlayNotVisible() {
        videoPlay.isNotVisible()
    }

    fun checkTimerIsVisible() {
        videoTimer.matches {
            withMatcher(MinimumAlphaMatcher(0.5f))
        }
    }

    fun checkVideoMuted(muted: Boolean) {
        videoMute.hasDrawable(
            if (muted) R.drawable.ic_mute else R.drawable.ic_unmute
        )
    }

    fun checkControlsNotVisible() {
        videoTimer.hasAlpha(0f)
        videoMute.hasAlpha(0f)
    }
}