package ru.auto.ara.core

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import ru.auto.ara.core.utils.createTestBitmap
import ru.auto.core_ui.util.image.GlideUrlImageLoader
import ru.auto.core_ui.util.image.IImageLoader
import ru.auto.core_ui.util.image.ImageLoaderParams

class TestImageLoader(
    private val predicate: (url: String) -> Boolean
) : IImageLoader {
    private val bitmap: Bitmap by lazy { createTestBitmap() }

    override fun load(
        url: String?,
        uri: Uri?,
        uriString: String?,
        resId: Int?,
        placeholder: Drawable?,
        error: Drawable?,
        animator: ValueAnimator?,
        target: ImageView,
        onFailed: () -> Unit,
        params: ImageLoaderParams?
    ) {
        if (url.checkPredicate() || uri?.toString().checkPredicate() || uriString.checkPredicate()) {
            target.setImageBitmap(bitmap)
        } else {
            GlideUrlImageLoader.load(
                url = url,
                uri = uri,
                uriString = uriString,
                resId = resId,
                placeholder = placeholder,
                error = error,
                animator = animator,
                target = target,
                onFailed = onFailed,
                params = params
            )
        }
    }

    private fun String?.checkPredicate() = this != null && predicate(this)

    override fun load(context: Context, url: String, onLoaded: (Bitmap) -> Unit, onFailed: () -> Unit) {
        if (predicate(url)) {
            onLoaded(bitmap)
        } else {
            GlideUrlImageLoader.load(
                context = context,
                url = url,
                onLoaded = onLoaded,
                onFailed = onFailed
            )
        }
    }
}
