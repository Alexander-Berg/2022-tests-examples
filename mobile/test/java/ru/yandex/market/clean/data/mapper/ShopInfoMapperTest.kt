package ru.yandex.market.clean.data.mapper

import com.annimon.stream.Exceptional
import org.assertj.core.api.Assertions
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.yandex.market.clean.data.fapi.dto.FrontApiMergedShopDto
import ru.yandex.market.clean.data.fapi.dto.FrontApiShopWorkScheduleDto
import ru.yandex.market.clean.data.fapi.dto.FrontApiShopWorkTimeDto
import ru.yandex.market.clean.data.fapi.dto.frontApiOperationalRatingDtoTestInstance
import ru.yandex.market.clean.data.mapper.shop.ShopDeliveryPricesMapper
import ru.yandex.market.clean.data.mapper.shop.ShopInfoMapper
import ru.yandex.market.clean.data.mapper.shop.ShopLogosMapper
import ru.yandex.market.clean.domain.model.WeekDay
import ru.yandex.market.clean.domain.model.cms.garson.ShopCmsWidgetGarsonSize
import ru.yandex.market.clean.domain.model.operationalRatingTestInstance
import ru.yandex.market.clean.domain.model.shop.ShopInfo
import ru.yandex.market.clean.domain.model.shop.ShopWorkSchedule
import ru.yandex.market.clean.domain.model.shop.ShopWorkScheduleTime
import ru.yandex.market.datetime.DateTimeProvider
import ru.yandex.market.domain.media.model.EmptyImageReference
import ru.yandex.market.net.sku.fapi.dto.pictureDtoTestInstance

class ShopInfoMapperTest {

    private val imageMapper = mock<ImageMapper> {
        on { mapImage(pictureDtoTestInstance(), false) } doReturn Exceptional.of { IMAGE_REF }
    }

    private val dateTimeProvider = mock<DateTimeProvider> {
        on { currentHour } doReturn CURRENT_HOUR
        on { currentMinute } doReturn CURRENT_MINUTE
    }

    private val shopLogosMapper: ShopLogosMapper = mock()
    private val shopDeliveryMapper: ShopDeliveryPricesMapper = mock()

    private val mapper = ShopInfoMapper(
        WeekDayMapper(),
        imageMapper,
        dateTimeProvider,
        OperationalRatingMapper(),
        shopLogosMapper,
        shopDeliveryMapper,
    )

    val dto = FrontApiMergedShopDto(
        id = SHOP_ID,
        name = SHOP_NAME,
        currentWorkSchedule = workSchedule,
        workScheduleList = List(7) { index -> workSchedule.copy(day = index) },
        logo = pictureDtoTestInstance(),
        ratingToShow = SHOP_RATING,
        brandColor = SHOP_BRAND_COLOR,
        entity = SHOP_ENTITY,
        hasSubsidies = SHOP_HAS_SUBSIDIES,
        operationalRatingId = SHOP_RATING_ID,
        operationalRating = SHOP_RATING_DTO,
        businessId = SHOP_BUSINESS_ID,
        shopGroup = null,
        gradesCount = GRADES_COUNT,
        newGradesCountPerAllTime = NEW_GRADES_COUNT_PER_ALL_TIME,
        newGradesCountPerThreeMonths = NEW_GRADES_COUNT_PER_THREE_MONTH,
        shopBrandName = SHOP_BRAND_NAME,
        logos = null,
        expressWarehouse = null
    )

    @Test
    fun `Should map correct with all fields`() {
        val workSchedule = ShopWorkSchedule(
            null,
            from = ShopWorkScheduleTime(hour = OPEN_HOUR, minute = OPEN_CLOSE_MINUTE),
            to = ShopWorkScheduleTime(hour = CLOSE_HOUR, minute = OPEN_CLOSE_MINUTE)
        )
        val workScheduleList = List(7) { workSchedule }.mapIndexed { index, _ ->
            when (index) {
                0 -> workSchedule.copy(day = WeekDay.MONDAY)
                1 -> workSchedule.copy(day = WeekDay.TUESDAY)
                2 -> workSchedule.copy(day = WeekDay.WEDNESDAY)
                3 -> workSchedule.copy(day = WeekDay.THURSDAY)
                4 -> workSchedule.copy(day = WeekDay.FRIDAY)
                5 -> workSchedule.copy(day = WeekDay.SATURDAY)
                6 -> workSchedule.copy(day = WeekDay.SUNDAY)
                else -> null
            }
        }
        val operationalRating = operationalRatingTestInstance(
            id = SHOP_RATING_ID,
            isHighTotalRate = false,
            isHighShipRate = false,
            isHighCancellationRate = false,
            isHighReturnRate = false,
            isHighCrossdockPlanFactRate = false,
            isHighFulfillmentPlanFactRate = false
        )
        val expected = ShopInfo(
            id = SHOP_ID,
            name = SHOP_NAME,
            logo = IMAGE_REF,
            rating = SHOP_RATING,
            currentWorkSchedule = workSchedule,
            isShopOpenNow = true,
            tomorrowWorkSchedule = workSchedule.copy(day = WeekDay.TUESDAY),
            workScheduleList = workScheduleList,
            brandColor = SHOP_BRAND_COLOR,
            operationalRating = operationalRating,
            businessId = SHOP_BUSINESS_ID,
            gradesPerThreeMonths = NEW_GRADES_COUNT_PER_THREE_MONTH,
            gradesPerAllTime = NEW_GRADES_COUNT_PER_ALL_TIME,
            shopBrandName = SHOP_BRAND_NAME,
            logos = null,
            delivery = null,
            garsonWidgetSize = ShopCmsWidgetGarsonSize.BIG,
        )
        val result = mapper.map(dto, ShopCmsWidgetGarsonSize.BIG)
        Assertions.assertThat(result).isEqualTo(expected)
    }


    private companion object {
        const val CURRENT_HOUR = 12
        const val CURRENT_MINUTE = 0
        const val OPEN_HOUR = 10
        const val CLOSE_HOUR = 19
        const val OPEN_CLOSE_MINUTE = 0
        const val SHOP_ID = 322L
        const val SHOP_NAME = "Solo shop"
        val workSchedule = FrontApiShopWorkScheduleDto(
            day = null,
            from = FrontApiShopWorkTimeDto(hour = OPEN_HOUR, minute = OPEN_CLOSE_MINUTE),
            to = FrontApiShopWorkTimeDto(hour = CLOSE_HOUR, minute = OPEN_CLOSE_MINUTE),
        )
        const val SHOP_RATING = 4.99
        const val SHOP_BRAND_COLOR = "BRAND color"
        const val SHOP_DELIVERY_TIME_MINUTES = 30
        const val SHOP_ENTITY = "entity"
        const val SHOP_HAS_SUBSIDIES = false
        const val SHOP_RATING_ID = "oper rating id"
        val SHOP_RATING_DTO = frontApiOperationalRatingDtoTestInstance(id = SHOP_RATING_ID)
        const val SHOP_BUSINESS_ID = 228L
        val IMAGE_REF = EmptyImageReference.instance

        const val GRADES_COUNT = 1000
        const val NEW_GRADES_COUNT_PER_ALL_TIME = 1000
        const val NEW_GRADES_COUNT_PER_THREE_MONTH = 500
        const val SHOP_BRAND_NAME = "Brand name"
    }
}
