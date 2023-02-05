package ru.yandex.market.data.deeplinks.links.product

import android.os.Build
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class ProductDeeplinkTest {

    @Test
    fun `Check deeplink is properly resolved with model id only`() {
        val modelId = 565434
        assertThat(ProductDeeplink.reverse(modelId.toString(), null)).isEqualTo("product/$modelId")
    }

    @Test
    fun `Check deeplink is properly resolved with model id and hid`() {
        val modelId = 123
        val hid = 456
        assertThat(ProductDeeplink.reverse(modelId.toString(), hid.toString())).isEqualTo("product/$modelId?$hid")
    }
}