package com.yandex.mail.shadows

import android.content.Context
import android.content.res.Resources
import android.graphics.Typeface
import android.util.TypedValue
import androidx.annotation.FontRes
import androidx.core.content.res.ResourcesCompat
import com.yandex.mail.util.NonInstantiableException
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

@Implements(ResourcesCompat::class) // https://github.com/robolectric/robolectric/issues/3286
class ShadowResourcesCompat private constructor() {

    init {
        throw NonInstantiableException()
    }

    companion object {

        @JvmStatic
        @Throws(Resources.NotFoundException::class)
        @Implementation
        fun getFont(context: Context, @FontRes id: Int): Typeface? {
            return Typeface.DEFAULT
        }

        @JvmStatic
        @Throws(Resources.NotFoundException::class)
        @Implementation
        fun getFont(
            context: Context,
            @FontRes id: Int,
            value: TypedValue,
            style: Int,
            fontCallback: ResourcesCompat.FontCallback?
        ): Typeface? {
            return Typeface.DEFAULT
        }
    }
}
