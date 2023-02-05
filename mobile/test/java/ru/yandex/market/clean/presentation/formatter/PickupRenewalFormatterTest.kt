package ru.yandex.market.clean.presentation.formatter

import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.beru.android.R
import ru.yandex.market.checkout.summary.AddressFormatter
import ru.yandex.market.common.dateformatter.DateFormatter
import ru.yandex.market.clean.presentation.feature.pickuprenewal.PickupRenewalState
import ru.yandex.market.clean.presentation.vo.PickupRenewalVo
import ru.yandex.market.data.passport.Address
import ru.yandex.market.common.android.ResourcesManager
import java.util.Date

class PickupRenewalFormatterTest {

    private val resourcesDataStore = mock<ResourcesManager> {
        on { getString(eq(R.string.pickup_renewal)) } doReturn PICKUP_RENEWAL
        on { getString(eq(R.string.pickup_renewal_thanks)) } doReturn PICKUP_RENEWAL_THANKS
        on { getString(eq(R.string.pickup_renewal_ok)) } doReturn PICKUP_RENEWAL_OK
        on { getString(eq(R.string.pickup_renewal_cancel)) } doReturn PICKUP_RENEWAL_CANCEL
        on { getString(eq(R.string.pickup_renewal_error_title)) } doReturn PICKUP_RENEWAL_ERROR_TITLE
        on { getString(eq(R.string.pickup_renewal_error_subtitle)) } doReturn PICKUP_RENEWAL_ERROR_SUBTITLE



        on { getFormattedString(eq(R.string.pickup_renewal_title), any()) }.thenAnswer { invocation ->
            val args = invocation.arguments
            String.format(PICKUP_RENEWAL_TITLE, args[1])
        }
        on { getFormattedString(eq(R.string.pickup_renewal_subtitle), any()) }.thenAnswer { invocation ->
            val args = invocation.arguments
            String.format(PICKUP_RENEWAL_SUBTITLE, args[1])
        }
        on { getFormattedString(eq(R.string.pickup_renewal_success_title), any()) }.thenAnswer { invocation ->
            val args = invocation.arguments
            String.format(PICKUP_RENEWAL_SUCCESS_TITLE, args[1])
        }
        on { getFormattedString(eq(R.string.pickup_renewal_success_subtitle), any()) }.thenAnswer { invocation ->
            val args = invocation.arguments
            String.format(PICKUP_RENEWAL_SUCCESS_SUBTITLE, args[1], args[2])
        }
    }
    private val addressFormatter = mock<AddressFormatter> {
        whenever(it.format(any(), any<Set<AddressFormatter.AddressComponent>>())).thenReturn(ADDRESS)
    }
    private val dateFormatter = mock<DateFormatter> {
        whenever(it.formatUntilShortPlusDayInWeekAtAccusative(any())).thenReturn(RENEWAL_DATE)
        whenever(it.formatShort(any())).thenReturn(CURRENT_DATE)
    }

    val formatter = PickupRenewalFormatter(
        resourcesDataStore,
        addressFormatter,
        dateFormatter,
    )

    @Test
    fun formatInitState() {
        val result = formatter.formatInitial(
            Date(),
            Date(),
        )

        val expected = PickupRenewalVo(
            title = PICKUP_RENEWAL_TITLE_FORMATTED,
            subtitle = PICKUP_RENEWAL_SUBTITLE_FORMATTED,
            actionButtonText = PICKUP_RENEWAL,
            subActionButtonText = PICKUP_RENEWAL_CANCEL,
            state = PickupRenewalState.INITIAL
        )

        assertEquals(expected, result)
    }

    @Test
    fun formatSuccessState() {
        val result = formatter.formatSuccess(
            ORDER_ID,
            Address.testInstance(),
            Date(),
        )

        val expected = PickupRenewalVo(
            title = PICKUP_RENEWAL_SUCCESS_TITLE_FORMATTED,
            subtitle = PICKUP_RENEWAL_SUCCESS_SUBTITLE_FORMATTED,
            actionButtonText = PICKUP_RENEWAL_THANKS,
            subActionButtonText = EMPTY,
            state = PickupRenewalState.SUCCESS
        )

        assertEquals(expected, result)
    }

    @Test
    fun formatErrorState() {
        val result = formatter.formatError()

        val expected = PickupRenewalVo(
            title = PICKUP_RENEWAL_ERROR_TITLE,
            subtitle = PICKUP_RENEWAL_ERROR_SUBTITLE,
            actionButtonText = PICKUP_RENEWAL_OK,
            subActionButtonText = EMPTY,
            state = PickupRenewalState.ERROR
        )

        assertEquals(expected, result)
    }

    companion object {
        private const val PICKUP_RENEWAL = "Продлить"
        private const val PICKUP_RENEWAL_THANKS = "Спасибо"
        private const val PICKUP_RENEWAL_OK = "Понятно"
        private const val PICKUP_RENEWAL_CANCEL = "Отменить"
        private const val PICKUP_RENEWAL_TITLE = "Продлить срок хранения %s?"
        private const val PICKUP_RENEWAL_SUBTITLE = "Или он закончится %s, и заказ вернётся на склад"
        private const val PICKUP_RENEWAL_ERROR_TITLE = "Простите, срок хранения не продлён"
        private const val PICKUP_RENEWAL_ERROR_SUBTITLE =
            "Попробуйте ещё раз позже"
        private const val PICKUP_RENEWAL_SUCCESS_TITLE = "Срок хранения продлён %s"
        private const val PICKUP_RENEWAL_SUCCESS_SUBTITLE = "Заказ %s ждёт вас в пункте самовывоза по адресу %s"
        private const val ADDRESS = "ул. Льва Толстого, 18Б"
        private const val CURRENT_DATE = "12 августа"
        private const val RENEWAL_DATE = "до понедельника, 21 августа"
        private const val ORDER_ID = "123411"


        private const val PICKUP_RENEWAL_TITLE_FORMATTED =
            "Продлить срок хранения до понедельника, 21 августа?"
        private const val PICKUP_RENEWAL_SUBTITLE_FORMATTED =
            "Или он закончится 12 августа, и заказ вернётся на склад"
        private const val PICKUP_RENEWAL_SUCCESS_TITLE_FORMATTED =
            "Срок хранения продлён до понедельника, 21 августа"
        private const val PICKUP_RENEWAL_SUCCESS_SUBTITLE_FORMATTED =
            "Заказ 123411 ждёт вас в пункте самовывоза по адресу ул. Льва Толстого, 18Б"
        private const val EMPTY = ""
    }
}