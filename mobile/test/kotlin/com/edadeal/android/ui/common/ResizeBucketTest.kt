package com.edadeal.android.ui.common

import okhttp3.HttpUrl
import kotlin.test.Test
import kotlin.test.assertEquals

class ResizeBucketTest {
    private val coverUrl = "https://yastatic.net/q/edadeal-leonardo/dyn/re/covers/orig/77478.jpg"
    private val itemUrl = "https://yastatic.net/q/edadeal-leonardo/dyn/re/items/24/orig/6277273.jpg"
    private val logoUrl = "https://yastatic.net/q/edadeal-leonardo/dyn/re/retailers/images/logos/sq/ret_724.png"

    @Test
    fun `getBestUrl should return nearest greater or equal size from bucket`() {
        val coverBucket = ResizeBucket.COVER
        val coverM = coverBucket.sizes.first { it.key == "m" }
        val coverL = coverBucket.sizes.first { it.key == "l" }
        val itemBucket = ResizeBucket.ITEM
        val itemM = itemBucket.sizes.first { it.key == "m" }
        val itemL = itemBucket.sizes.first { it.key == "l" }
        val logoBucket = ResizeBucket.RETAILER_LOGO
        val logoS = logoBucket.sizes.first { it.key == "s" }
        val logoM = logoBucket.sizes.first { it.key == "m" }
        val logoL = logoBucket.sizes.first { it.key == "l" }

        assertEquals(itemM.key, itemBucket.getBestResKey(itemUrl, 264, 264))
        assertEquals(itemM.key, itemBucket.getBestResKey(itemUrl, 0, 264))
        assertEquals(itemM.key, itemBucket.getBestResKey(itemUrl, 264, 0))
        assertEquals(itemL.key, itemBucket.getBestResKey(itemUrl, 396, 396))
        assertEquals(itemL.key, itemBucket.getBestResKey(itemUrl, 0, 396))
        assertEquals(itemL.key, itemBucket.getBestResKey(itemUrl, 396, 0))
        assertEquals(coverM.key, coverBucket.getBestResKey(coverUrl, coverM.width, coverM.height))
        assertEquals(coverL.key, coverBucket.getBestResKey(coverUrl, coverM.width + 8, coverM.height + 8))
        assertEquals(coverL.key, coverBucket.getBestResKey(coverUrl, 9999, 9999))
        assertEquals(logoS.key, logoBucket.getBestResKey(logoUrl, 0, 0))
        assertEquals(logoS.key, logoBucket.getBestResKey(logoUrl, 96, 96))
        assertEquals(logoM.key, logoBucket.getBestResKey(logoUrl, logoM.width, logoM.height))
        assertEquals(logoL.key, logoBucket.getBestResKey(logoUrl, logoM.width + 8, logoM.height + 8))
        assertEquals(logoL.key, logoBucket.getBestResKey(logoUrl, 9999, 9999))
    }

    @Test
    fun `getBestUrl should return original value if it doesn't look like url or bucket is not specified`() {
        val notUrlAtAll = "scheme\\\\path"

        assertEquals(notUrlAtAll, ResizeBucket.COVER.getBestUrl(notUrlAtAll, 9999, 9999))
        assertEquals("", ResizeBucket.ITEM.getBestUrl("", 9999, 9999))
        assertEquals(coverUrl, ResizeBucket.NONE.getBestUrl(coverUrl, 9999, 9999))
    }

    private fun ResizeBucket.getBestResKey(url: String, width: Int, height: Int): String {
        val bestUrl = getBestUrl(url, width, height)
        return HttpUrl.get(bestUrl).queryParameter("res").orEmpty()
    }
}
