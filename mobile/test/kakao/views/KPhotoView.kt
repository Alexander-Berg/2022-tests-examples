package ru.yandex.market.test.kakao.views

import android.content.res.Resources
import androidx.test.espresso.ViewAssertion
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import com.github.chrisbanes.photoview.PhotoView


class KPhotoView(function: ViewBuilder.() -> Unit) : KBaseView<KPhotoView>(function) {
    fun checkZoom(isZoomed: Boolean) {
        view.check(ViewAssertion { view, noViewFoundException ->
            if (view is PhotoView) {
                if (view.attacher.scale <= view.attacher.minimumScale == isZoomed) {
                    throw AssertionError(
                        "Image is not zoomed, scale: ${view.attacher.scale}. " +
                                "maximumScale: ${view.attacher.maximumScale}." +
                                "minimumScale: ${view.attacher.minimumScale}." +
                                "mediumScale: ${view.attacher.mediumScale}."
                    )
                }
            } else {
                noViewFoundException?.let { throw throw AssertionError(it) }
            }
        })
    }

    fun checkFullScreen(isFullScreen: Boolean) {
        view.check(ViewAssertion { view, noViewFoundException ->
            if (view is PhotoView) {
                val metrics = Resources.getSystem()?.displayMetrics
                    ?: throw NullPointerException("Can't find displayMetrics")
                val context = view.context

                var statusBarHeight = 0
                val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
                if (resourceId > 0) {
                    statusBarHeight = context.resources.getDimensionPixelSize(resourceId)
                }

                if ((isFullScreen && (view.width != metrics.widthPixels || view.height != metrics.heightPixels - statusBarHeight))
                    || (!isFullScreen && (view.width == metrics.widthPixels || view.height == metrics.heightPixels - statusBarHeight))
                ) {
                    throw AssertionError("View is not fullscreen. View size: ${view.width}x${view.height}, screen size: ${metrics.widthPixels}x${metrics.heightPixels}")
                }
            } else {
                noViewFoundException?.let { throw throw AssertionError(it) }
            }
        })
    }
}
