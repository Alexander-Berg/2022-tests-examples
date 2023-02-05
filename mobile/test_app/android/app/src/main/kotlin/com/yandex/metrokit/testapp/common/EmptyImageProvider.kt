package com.yandex.metrokit.testapp.common

import android.graphics.Bitmap
import com.yandex.runtime.image.ImageProvider

// FIXME: Workaround because runtime-generated bindings cannot accept null ImageProvider
object EmptyImageProvider : ImageProvider() {
    override fun getId(): String = ""
    override fun getImage(): Bitmap = emptyBitmap

    private val emptyBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
}
