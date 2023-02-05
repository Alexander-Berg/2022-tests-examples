package ru.yandex.market.clean.presentation.feature.purchasebylist.map.chips.courier

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.beru.android.R
import ru.yandex.market.clean.domain.model.purchasebylist.DeliveryPeriod
import ru.yandex.market.clean.presentation.feature.purchaseByList.map.chips.courier.DeliveryPeriodToReadableNameFormatter
import ru.yandex.market.common.android.ResourcesManager
import ru.yandex.market.optional.Optional
import ru.yandex.market.utils.Characters
import ru.yandex.market.utils.orNull

class DeliveryPeriodToReadableNameFormatterTest {

    private val resourceManager = mock<ResourcesManager> {
        on { getString(R.string.in_one_two_hours) } doReturn IN_ONE_TWO_HOURS
        on { getString(R.string.express) } doReturn EXPRESS
        on { getString(R.string.today) } doReturn TODAY
        on { getString(R.string.arrive_today) } doReturn ARRIVE_TODAY
        on { getString(R.string.arrive_tomorrow) } doReturn ARRIVE_TOMORROW
        on { getString(R.string.tomorrow) } doReturn TOMORROW
        on { getString(R.string.courier_delivery_type) } doReturn COURIER_DELIVERY_TYPE
        on { getString(DELIVERY_PERIOD_OTHER_NAME_RES) } doReturn DELIVERY_PERIOD_OTHER_NAME
        on { getString(DELIVERY_PERIOD_NULL_NAME_RES) } doReturn DELIVERY_PERIOD_NULL_NAME
    }

    private val formatter = DeliveryPeriodToReadableNameFormatter(
        resourcesManager = resourceManager,
    )

    @Test
    fun `Format default delivery period readable name`() {
        val inOneTwoHours = formatter.format(isBookingEnabled = false, NON_NULL_EXPRESS_DELIVERY_PERIOD)
        val express = formatter.format(isBookingEnabled = true, NON_NULL_EXPRESS_DELIVERY_PERIOD)
        val today = formatter.format(isBookingEnabled = false, NON_NULL_TODAY_DELIVERY_PERIOD)
        val tomorrow = formatter.format(isBookingEnabled = false, NON_NULL_NEXT_DAY_DELIVERY_PERIOD)
        val other = formatter.format(isBookingEnabled = false, NON_NULL_OTHER_DELIVERY_PERIOD)

        assertThat(inOneTwoHours).isEqualTo(IN_ONE_TWO_HOURS)
        assertThat(express).isEqualTo(EXPRESS)
        assertThat(today).isEqualTo(TODAY)
        assertThat(tomorrow).isEqualTo(TOMORROW)
        assertThat(other).isEqualTo(COURIER_DELIVERY_TYPE)
    }

    @Test
    fun `Format nullable delivery period readable name with other name`() {
        val inOneTwoHours = formatter.format(
            deliveryPeriod = NON_NULL_EXPRESS_DELIVERY_PERIOD,
            deliveryPeriodOtherName = DELIVERY_PERIOD_OTHER_NAME_RES,
            deliveryPeriodNullName = DELIVERY_PERIOD_NULL_NAME_RES,
        )
        val today = formatter.format(
            deliveryPeriod = NON_NULL_TODAY_DELIVERY_PERIOD,
            deliveryPeriodOtherName = DELIVERY_PERIOD_OTHER_NAME_RES,
            deliveryPeriodNullName = DELIVERY_PERIOD_NULL_NAME_RES,
        )
        val tomorrow = formatter.format(
            deliveryPeriod = NON_NULL_NEXT_DAY_DELIVERY_PERIOD,
            deliveryPeriodOtherName = DELIVERY_PERIOD_OTHER_NAME_RES,
            deliveryPeriodNullName = DELIVERY_PERIOD_NULL_NAME_RES,
        )
        val other = formatter.format(
            deliveryPeriod = NON_NULL_OTHER_DELIVERY_PERIOD,
            deliveryPeriodOtherName = DELIVERY_PERIOD_OTHER_NAME_RES,
            deliveryPeriodNullName = DELIVERY_PERIOD_NULL_NAME_RES,
        )
        val nullable = formatter.format(
            deliveryPeriod = null,
            deliveryPeriodOtherName = DELIVERY_PERIOD_OTHER_NAME_RES,
            deliveryPeriodNullName = DELIVERY_PERIOD_NULL_NAME_RES,
        )

        assertThat(inOneTwoHours).isEqualTo(IN_ONE_TWO_HOURS_LOWER_CASE)
        assertThat(today).isEqualTo(ARRIVE_TODAY)
        assertThat(tomorrow).isEqualTo(ARRIVE_TOMORROW)
        assertThat(other).isEqualTo(DELIVERY_PERIOD_OTHER_NAME)
        assertThat(nullable).isEqualTo(DELIVERY_PERIOD_NULL_NAME)
    }

    @Test
    fun `Format nullable delivery period readable name without other name`() {
        val inOneTwoHours = formatter.format(
            deliveryPeriod = NULLABLE_EXPRESS_DELIVERY_PERIOD,
        )
        val today = formatter.format(
            deliveryPeriod = NULLABLE_TODAY_DELIVERY_PERIOD,
        )
        val tomorrow = formatter.format(
            deliveryPeriod = NULLABLE_NEXT_DAY_DELIVERY_PERIOD,
        )
        val other = formatter.format(
            deliveryPeriod = NULLABLE_OTHER_DELIVERY_PERIOD,
        )
        val nullable = formatter.format(
            deliveryPeriod = null,
        )

        assertThat(inOneTwoHours).isEqualTo(IN_ONE_TWO_HOURS_LOWER_CASE)
        assertThat(today).isEqualTo(ARRIVE_TODAY)
        assertThat(tomorrow).isEqualTo(ARRIVE_TOMORROW)
        assertThat(other).isNull()
        assertThat(nullable).isNull()
    }

    private companion object {
        val NON_NULL_EXPRESS_DELIVERY_PERIOD = DeliveryPeriod.EXPRESS
        val NON_NULL_TODAY_DELIVERY_PERIOD = DeliveryPeriod.TODAY
        val NON_NULL_NEXT_DAY_DELIVERY_PERIOD = DeliveryPeriod.NEXT_DAY
        val NON_NULL_OTHER_DELIVERY_PERIOD = DeliveryPeriod.OTHER

        val NULLABLE_EXPRESS_DELIVERY_PERIOD = Optional.ofNullable(DeliveryPeriod.EXPRESS).orNull
        val NULLABLE_TODAY_DELIVERY_PERIOD = Optional.ofNullable(DeliveryPeriod.TODAY).orNull
        val NULLABLE_NEXT_DAY_DELIVERY_PERIOD = Optional.ofNullable(DeliveryPeriod.NEXT_DAY).orNull
        val NULLABLE_OTHER_DELIVERY_PERIOD = Optional.ofNullable(DeliveryPeriod.OTHER).orNull

        const val DELIVERY_PERIOD_OTHER_NAME_RES = R.string.here
        const val DELIVERY_PERIOD_OTHER_NAME = "пример 1"
        const val DELIVERY_PERIOD_NULL_NAME_RES = R.string.purchase_by_list_full_cart
        const val DELIVERY_PERIOD_NULL_NAME = "пример 2"

        const val IN_ONE_TWO_HOURS = "Через 1${Characters.EN_DASH}2 часа"
        const val EXPRESS = "Экспресс"
        const val IN_ONE_TWO_HOURS_LOWER_CASE = "через 1${Characters.EN_DASH}2 часа"
        const val TODAY = "Сегодня"
        const val ARRIVE_TODAY = "сегодня"
        const val TOMORROW = "Завтра"
        const val ARRIVE_TOMORROW = "завтра"
        const val COURIER_DELIVERY_TYPE = "Доставка"
    }
}
