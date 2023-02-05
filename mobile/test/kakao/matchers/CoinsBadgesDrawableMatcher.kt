package ru.yandex.market.test.kakao.matchers

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import android.view.View
import androidx.collection.LruCache
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import ru.yandex.market.feature.price.ui.SmartCoinBadgeStackDrawable

class CoinsBadgesDrawableMatcher(private val coinsColorsRgb: List<String>) : TypeSafeMatcher<View>() {

    private val smartCoinBitmapsCache =
        LruCache<SmartCoinBadgeStackDrawable.CacheKey, Bitmap>(coinsColorsRgb.size)

    override fun describeTo(description: Description?) {
        description?.appendText("View background is coins badges vs colors $coinsColorsRgb")
    }

    override fun matchesSafely(item: View?): Boolean {
        return (item?.background as? SmartCoinBadgeStackDrawable)?.let { actualDrawable ->
            val expectedDrawable = SmartCoinBadgeStackDrawable(item.context, smartCoinBitmapsCache)
            expectedDrawable.badgeColors = coinsColorsRgb.map(Color::parseColor)
            val actualBitmap = drawableToBitmap(actualDrawable)
            val expectedBitmap = drawableToBitmap(expectedDrawable)

            return actualBitmap.sameAs(expectedBitmap)
        } ?: false
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            if (drawable.bitmap != null) {
                return drawable.bitmap
            }
        }

        if (drawable is StateListDrawable) {
            if (drawable.getCurrent() is BitmapDrawable) {
                val bitmapDrawable = drawable.getCurrent() as BitmapDrawable
                if (bitmapDrawable.bitmap != null) {
                    return bitmapDrawable.bitmap
                }
            }
        }

        val bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888) // Single color bitmap will be created of 1x1 pixel
        } else {
            Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        }

        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}