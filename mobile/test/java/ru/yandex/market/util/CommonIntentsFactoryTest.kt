package ru.yandex.market.util

import android.content.Intent
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import org.hamcrest.Matchers.allOf
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.HamcrestCondition
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class CommonIntentsFactoryTest {

    private val factory = CommonIntentsFactory(ApplicationProvider.getApplicationContext())

    @Test
    fun `Creates yandex maps route to place intent as expected`() {
        val latitude = 55.733842
        val longitude = 55.760090
        val intent = factory.createYandexMapsRouteToPlaceIntent(latitude, longitude)

        assertThat(intent).`is`(
            HamcrestCondition(
                allOf(
                    hasAction(Intent.ACTION_VIEW),
                    hasData("https://yandex.ru/maps/?rtext=~$latitude%2C$longitude&rtt=auto")
                )
            )
        )
    }
}