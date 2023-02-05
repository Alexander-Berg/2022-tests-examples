package ru.yandex.market.test.kakao.views

import android.view.View
import androidx.annotation.IdRes
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.view.isVisible
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import com.google.android.exoplayer2.ui.PlayerControlView
import org.hamcrest.Matcher
import ru.beru.android.R

class KPlayerControlView : KBaseView<KPlayerControlView> {
    constructor(function: ViewBuilder.() -> Unit) : super(function)
    constructor(parent: Matcher<View>, function: ViewBuilder.() -> Unit) : super(parent, function)

    fun clickForward() {
        findPlayerControlViewById<AppCompatImageButton>(R.id.exo_ffwd) { it.performClick() }
    }

    fun clickBack() {
        findPlayerControlViewById<AppCompatImageButton>(R.id.exo_rew) { it.performClick() }
    }

    fun clickPlay() {
        findPlayerControlViewById<AppCompatImageButton>(R.id.exo_play) { it.performClick() }
    }

    fun clickPause() {
        findPlayerControlViewById<AppCompatImageButton>(R.id.exo_pause) { it.performClick() }
    }

    fun isPlayVisible() {
        findPlayerControlViewById<AppCompatImageButton>(R.id.exo_play) { it.isVisible }
    }

    private fun <T : View> findPlayerControlViewById(@IdRes res: Int, action: ((T) -> Unit)? = null) {
        view.check { view, notFoundException ->
            if (view is PlayerControlView) {
                view.findViewById<T>(res)?.let { action?.invoke(it) }
                    ?: throw AssertionError("View with $res was not found")
            } else {
                notFoundException.let { throw AssertionError(it) }
            }
        }
    }
}