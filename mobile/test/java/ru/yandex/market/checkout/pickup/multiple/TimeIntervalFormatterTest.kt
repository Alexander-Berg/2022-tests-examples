package ru.yandex.market.checkout.pickup.multiple

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.market.checkout.delivery.address.TimeFormatter
import ru.yandex.market.common.LocalTime
import ru.yandex.market.clean.domain.model.TimeInterval
import ru.yandex.market.common.android.ResourcesManagerImpl

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class TimeIntervalFormatterTest(
    private val input: TimeInterval,
    private val expectedOutput: String
) {

    private val formatter = TimeIntervalFormatter(
        ResourcesManagerImpl(ApplicationProvider.getApplicationContext<Context>().resources),
        TimeFormatter()
    )

    @Test
    fun `Properly maps valid input`() {
        val formatted = formatter.format(input)
        assertThat(formatted).isEqualTo(expectedOutput)
    }

    companion object {

        @ParameterizedRobolectricTestRunner.Parameters
        @JvmStatic
        fun parameters(): Iterable<Array<*>> = listOf(
            arrayOf(
                TimeInterval.create(LocalTime.midnight(), LocalTime.dayEnd()),
                "Круглосуточно"
            ),
            arrayOf(
                TimeInterval.create(
                    LocalTime.create(10, 0),
                    LocalTime.create(21, 0)
                ),
                "10:00\u200A\u2013\u200A21:00"
            ),
            arrayOf(
                TimeInterval.create(
                    LocalTime.create(10, 0),
                    LocalTime.dayEnd()
                ),
                "10:00\u200A\u2013\u200A24:00"
            ),
            arrayOf(
                TimeInterval.create(
                    LocalTime.create(10, 0),
                    LocalTime.create(23, 59)
                ),
                "10:00\u200A\u2013\u200A24:00"
            )
        )
    }
}
