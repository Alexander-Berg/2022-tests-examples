package ru.auto.ara.core

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.RequestManager
import ru.auto.ara.core.utils.createTestBitmap
import ru.auto.ara.util.createCustomTarget
import ru.auto.core_ui.util.image.ImageLoadStrategy

class TestImageLoadStrategy(
    private val predicate: (url: String) -> Boolean
) : ImageLoadStrategy {
    private val bitmap: Bitmap by lazy { createTestBitmap() }

    override fun apply(requestBuilder: RequestBuilder<Bitmap>, url: String, imageView: ImageView) {
        if (predicate(url)) {
            requestBuilder.load(bitmap).into(imageView)
        } else {
            requestBuilder.load(url).into(imageView)
        }
    }

    override fun apply(requestBuilder: RequestBuilder<Bitmap>, uri: Uri, imageView: ImageView) {
        if (predicate(uri.toString())) {
            requestBuilder.load(bitmap).into(imageView)
        } else {
            requestBuilder.load(uri).into(imageView)
        }
    }

    override fun apply(
        requestBuilder: RequestBuilder<Bitmap>,
        url: String,
        onLoaded: (Bitmap) -> Unit,
        onFailed: () -> Unit,
    ) {
        if (predicate(url)) {
            requestBuilder.load(bitmap).into(createCustomTarget(onLoaded, onFailed))
        } else {
            requestBuilder.load(url).into(createCustomTarget(onLoaded, onFailed))
        }
    }

    override fun apply(requestManager: RequestManager, url: String): RequestBuilder<Drawable> =
        if (predicate(url)) {
            requestManager.load(bitmap)
        } else {
            requestManager.load(url)
        }

}
