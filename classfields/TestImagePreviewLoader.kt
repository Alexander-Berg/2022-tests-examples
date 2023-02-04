package ru.auto.ara.core

import android.graphics.Bitmap
import android.widget.ImageView
import ru.auto.ara.core.utils.createTestBitmap
import ru.auto.core_ui.util.image.IImagePreviewLoader
import ru.auto.core_ui.util.image.MultisizeImage
import ru.auto.data.model.data.offer.PhotoPreview

class TestImagePreviewLoader : IImagePreviewLoader {
    val bitmap: Bitmap by lazy { createTestBitmap() }

    override fun loadImage(imageView: ImageView, url: String, preview: PhotoPreview?, placeholder: Int?) {
        imageView.setImageBitmap(bitmap)
    }

    override fun loadImage(imageView: ImageView, image: MultisizeImage, placeholder: Int?) {
        imageView.setImageBitmap(bitmap)
    }

    override fun onRecycled() {
        // Not implemented.
    }

}
