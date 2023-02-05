package ru.yandex.market.clean.presentation.feature.purchasebylist

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.market.clean.presentation.feature.purchaseByList.map.filters.PurchaseByListFilter
import ru.yandex.market.clean.presentation.feature.purchaseByList.map.filters.PurchaseByListFilterFormatter
import ru.yandex.market.clean.presentation.feature.purchaseByList.map.filters.PurchaseByListFilterItemVo
import ru.yandex.market.clean.presentation.feature.purchaseByList.map.filters.PurchaseByListFiltersDialogFragment.PharmaBookingConfigParcelable
import ru.yandex.market.clean.presentation.feature.purchaseByList.map.filters.PurchaseByListFiltersFormatterParams
import ru.yandex.market.common.android.ResourcesManagerImpl

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P], qualifiers = "ru")
class PurchaseByListFilterFormatterTest {

    private val resourceManager = ResourcesManagerImpl(ApplicationProvider.getApplicationContext<Context>().resources)

    private val formatter = PurchaseByListFilterFormatter(
        resourcesManager = resourceManager
    )

    private val params = PurchaseByListFiltersFormatterParams(
        hasFullPickupPoint = false,
        hasPromoCodePoint = false,
        pharmaBookingConfig = PharmaBookingConfigParcelable(isEnabled = true, bookingTime = null)
    )

    @Test
    fun `should create vo with express and booking time filters when booking toggle enabled`() {
        val selectedFilters = emptySet<PurchaseByListFilter>()
        val params = params.changeBookingParams(isBookingEnabled = true)
        val expected = setOf(
            TIME_GROUP_HEADER_VO,
            ANY_TIME_VO.copy(isSelected = true),
            BOOKING_VO,
            EXPRESS_VO
        )
        val actual = formatter.formatFilters(
            selectedFilters,
            params,
            PurchaseByListFilter.ExpressDelivery,
            PurchaseByListFilter.InOneThreeDaysDelivery
        )

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `should not create time filters when booking toggle turned off`() {
        val selectedFilters = emptySet<PurchaseByListFilter>()
        val params = params.changeBookingParams(isBookingEnabled = false)
        val expected = emptySet<PurchaseByListFilterItemVo>()

        val actual = formatter.formatFilters(
            selectedFilters,
            params,
            PurchaseByListFilter.InOneThreeDaysDelivery,
            PurchaseByListFilter.ExpressDelivery
        )

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `should select time filter that was preselected`() {
        val selectedFilters = setOf<PurchaseByListFilter>(PurchaseByListFilter.InOneThreeDaysDelivery)
        val params = params.changeBookingParams(isBookingEnabled = true)
        val expected = setOf(
            TIME_GROUP_HEADER_VO,
            ANY_TIME_VO.copy(isSelected = false),
            BOOKING_VO,
            EXPRESS_VO.copy(isSelected = true)
        )

        val actual = formatter.formatFilters(
            selectedFilters,
            params,
            PurchaseByListFilter.ExpressDelivery,
            PurchaseByListFilter.InOneThreeDaysDelivery
        )

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `AnyTimeBooking should be selected if any other time filter was not preselected`() {
        val selectedFilters = emptySet<PurchaseByListFilter>()
        val params = params.changeBookingParams(isBookingEnabled = true)
        val expected = setOf(
            TIME_GROUP_HEADER_VO,
            ANY_TIME_VO.copy(isSelected = true),
            BOOKING_VO,
            EXPRESS_VO
        )

        val actual = formatter.formatFilters(
            selectedFilters,
            params,
            PurchaseByListFilter.ExpressDelivery,
            PurchaseByListFilter.InOneThreeDaysDelivery
        )

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `should now show time filters if only one exists`() {
        val selectedFilters = setOf<PurchaseByListFilter>()
        val params = params.changeBookingParams(isBookingEnabled = true)
        val expected = emptySet<PurchaseByListFilterItemVo>()

        val actual = formatter.formatFilters(
            selectedFilters,
            params,
            PurchaseByListFilter.InOneThreeDaysDelivery,
        )

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `should create filters with time and feature type and group by priority when time is first`() {
        val selectedFilters = setOf(PurchaseByListFilter.Daily, PurchaseByListFilter.AroundTheClock)
        val params = params.copy(
            pharmaBookingConfig = PharmaBookingConfigParcelable(true, null),
            hasFullPickupPoint = true,
            hasPromoCodePoint = true
        )
        val expected = setOf(
            TIME_GROUP_HEADER_VO,
            ANY_TIME_VO.copy(isSelected = true),
            BOOKING_VO,
            EXPRESS_VO,
            FEATURE_GROUP_HEADER_VO,
            PurchaseByListFilterItemVo.FilterVo.FeatureFilterVo(
                type = PurchaseByListFilter.Daily,
                title = "Работают без выходных",
                isSelected = true
            ),
            PurchaseByListFilterItemVo.FilterVo.FeatureFilterVo(
                type = PurchaseByListFilter.AroundTheClock,
                title = "Круглосуточные",
                isSelected = true
            ),
            PurchaseByListFilterItemVo.FilterVo.FeatureFilterVo(
                type = PurchaseByListFilter.FullPoint,
                title = "Со всеми лекарствами",
                isSelected = false
            ),
            PurchaseByListFilterItemVo.FilterVo.FeatureFilterVo(
                type = PurchaseByListFilter.WithPromo,
                title = "Принимает промокод",
                isSelected = false
            )
        )

        val actual = formatter.formatFilters(
            selectedFilters,
            params,
            PurchaseByListFilter.Daily,
            PurchaseByListFilter.AroundTheClock,
            PurchaseByListFilter.FullPoint,
            PurchaseByListFilter.WithPromo,
            PurchaseByListFilter.ExpressDelivery,
            PurchaseByListFilter.InOneThreeDaysDelivery,
        )

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `should not add FullPoint if hasFullPickupPoint = false`() {
        val selectedFilters = emptySet<PurchaseByListFilter>()
        val params = params.changeBookingParams(isBookingEnabled = true)
        val expected = emptySet<PurchaseByListFilter>()

        val actual = formatter.formatFilters(
            selectedFilters,
            params,
            PurchaseByListFilter.FullPoint,
        )

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `should not add WithPromo if hasFullPickupPoint = false`() {
        val selectedFilters = emptySet<PurchaseByListFilter>()
        val params = params.changeBookingParams(isBookingEnabled = true)
        val expected = emptySet<PurchaseByListFilter>()

        val actual = formatter.formatFilters(
            selectedFilters,
            params,
            PurchaseByListFilter.WithPromo,
        )

        assertThat(actual).isEqualTo(expected)
    }

    private fun PurchaseByListFiltersFormatterParams.changeBookingParams(
        isBookingEnabled: Boolean,
        bookingTime: String? = null
    ): PurchaseByListFiltersFormatterParams {
        return copy(
            pharmaBookingConfig = PharmaBookingConfigParcelable(
                isEnabled = isBookingEnabled,
                bookingTime = bookingTime
            )
        )
    }

    private companion object {
        val ANY_TIME_VO = PurchaseByListFilterItemVo.FilterVo.BookingFilterVo(
            type = PurchaseByListFilter.AnyTimeBooking,
            title = "Любой срок",
            isSelected = false
        )

        val EXPRESS_VO = PurchaseByListFilterItemVo.FilterVo.BookingFilterVo(
            type = PurchaseByListFilter.InOneThreeDaysDelivery,
            title = "От 1 до 3 дней",
            isSelected = false
        )

        val BOOKING_VO = PurchaseByListFilterItemVo.FilterVo.BookingFilterVo(
            type = PurchaseByListFilter.ExpressDelivery,
            title = "Через 1–2 часа",
            isSelected = false
        )

        val TIME_GROUP_HEADER_VO = PurchaseByListFilterItemVo.GroupHeaderVo("Доставка в аптеку")
        val FEATURE_GROUP_HEADER_VO = PurchaseByListFilterItemVo.GroupHeaderVo("Аптеки")

    }
}
