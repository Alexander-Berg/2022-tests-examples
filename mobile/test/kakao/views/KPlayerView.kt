package ru.yandex.market.test.kakao.views

import android.view.View
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import org.hamcrest.Matcher

class KPlayerView : KBaseView<KPlayerView> {
    constructor(function: ViewBuilder.() -> Unit) : super(function)
    constructor(parent: Matcher<View>, function: ViewBuilder.() -> Unit) : super(parent, function)

    fun checkVideoIsPlaying() {
        view.check { view, notFoundException ->
            if (view is PlayerView) {
                if (view.player?.playbackState != Player.STATE_READY) {
                    throw AssertionError("Player expected to play video but it's not. Actual state ${view.player?.playbackState}")
                }
            } else {
                notFoundException.let { throw AssertionError(it) }
            }
        }
    }
}