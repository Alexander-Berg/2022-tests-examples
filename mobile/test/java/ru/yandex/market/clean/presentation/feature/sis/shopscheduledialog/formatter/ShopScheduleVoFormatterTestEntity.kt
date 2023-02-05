package ru.yandex.market.clean.presentation.feature.sis.shopscheduledialog.formatter

import ru.yandex.market.clean.domain.model.WeekDay
import ru.yandex.market.clean.domain.model.cms.garson.ShopCmsWidgetGarsonSize
import ru.yandex.market.clean.domain.model.shop.ShopInfo
import ru.yandex.market.clean.domain.model.shop.ShopWorkSchedule
import ru.yandex.market.clean.domain.model.shop.ShopWorkScheduleTime
import ru.yandex.market.clean.domain.model.shop.shopWorkScheduleTimeTestInstance
import ru.yandex.market.clean.presentation.feature.sis.shopscheduledialog.vo.ShopScheduleTimePeriodVo
import ru.yandex.market.clean.presentation.feature.sis.shopscheduledialog.vo.ShopScheduleVo
import ru.yandex.market.domain.media.model.uCropImageReferenceTestInstance

object ShopScheduleVoFormatterTestEntity {

    const val DIALOG_SHOP_SCHEDULE_TITLE = "График работы"
    const val DAILY = "Ежедневно"
    const val WEEKDAYS = "Будни"
    const val WEEKENDS = "Выходные"
    private const val FROM = "с"
    private const val TO = "до"
    const val WHOLE_DAY = "Круглосуточно"

    const val MONDAY = "Понедельник"
    const val TUESDAY = "Вторник"
    const val WEDNESDAY = "Среда"
    const val THURSDAY = "Четверг"
    const val FRIDAY = "Пятница"
    const val SATURDAY = "Суббота"
    const val SUNDAY = "Воскресенье"

    private const val ID = 1L
    private const val NAME = "name"
    private val LOGO = uCropImageReferenceTestInstance()
    private const val RATING = 0.5

    val SHOP_WORK_SCHEDULE_TIME_START = shopWorkScheduleTimeTestInstance(12, 30)
    val SHOP_WORK_SCHEDULE_TIME_END = shopWorkScheduleTimeTestInstance(18, 20)
    val SHOP_WORK_SCHEDULE_TIME_PERIOD_START =
        "${SHOP_WORK_SCHEDULE_TIME_START.hour ?: 0}:${SHOP_WORK_SCHEDULE_TIME_START.minute ?: 0}"
    val SHOP_WORK_SCHEDULE_TIME_PERIOD_END =
        "${SHOP_WORK_SCHEDULE_TIME_END.hour ?: 0}:${SHOP_WORK_SCHEDULE_TIME_END.minute ?: 0}"
    val SHOP_SCHEDULE_EVERYDAY_FROM_TO =
        "$FROM $SHOP_WORK_SCHEDULE_TIME_PERIOD_START $TO $SHOP_WORK_SCHEDULE_TIME_PERIOD_END"
    val SHOP_SCHEDULE_EVERYDAY_PERIOD =
        "$SHOP_WORK_SCHEDULE_TIME_PERIOD_START — $SHOP_WORK_SCHEDULE_TIME_PERIOD_END"

    val SHOP_WORK_SCHEDULE_TIME_MIDDAY_START = ShopWorkScheduleTime(hour = 12, minute = 10)
    val SHOP_WORK_SCHEDULE_TIME_MIDDAY_END = ShopWorkScheduleTime(hour = 17, minute = 10)
    val SHOP_WORK_SCHEDULE_TIME_MIDDAY_PERIOD_START =
        "${SHOP_WORK_SCHEDULE_TIME_MIDDAY_START.hour ?: 0}:${SHOP_WORK_SCHEDULE_TIME_MIDDAY_START.minute ?: 0}"
    val SHOP_WORK_SCHEDULE_TIME_MIDDAY_PERIOD_END =
        "${SHOP_WORK_SCHEDULE_TIME_MIDDAY_END.hour ?: 0}:${SHOP_WORK_SCHEDULE_TIME_MIDDAY_END.minute ?: 0}"
    val SHOP_SCHEDULE_EVERYDAY_MIDDAY_PERIOD =
        "$SHOP_WORK_SCHEDULE_TIME_MIDDAY_PERIOD_START — $SHOP_WORK_SCHEDULE_TIME_MIDDAY_PERIOD_END"

    private val SHOP_WORK_SCHEDULE_MONDAY = ShopWorkSchedule(
        day = WeekDay.MONDAY,
        from = SHOP_WORK_SCHEDULE_TIME_START,
        to = SHOP_WORK_SCHEDULE_TIME_END,
    )
    private val SHOP_WORK_SCHEDULE_MONDAY_MIDDAY = ShopWorkSchedule(
        day = WeekDay.MONDAY,
        from = SHOP_WORK_SCHEDULE_TIME_MIDDAY_START,
        to = SHOP_WORK_SCHEDULE_TIME_MIDDAY_END,
    )
    private val SHOP_WORK_SCHEDULE_TUESDAY = SHOP_WORK_SCHEDULE_MONDAY.copy(day = WeekDay.TUESDAY)
    private val SHOP_WORK_SCHEDULE_WEDNESDAY = SHOP_WORK_SCHEDULE_MONDAY.copy(day = WeekDay.WEDNESDAY)
    private val SHOP_WORK_SCHEDULE_THURSDAY = SHOP_WORK_SCHEDULE_MONDAY.copy(day = WeekDay.THURSDAY)
    private val SHOP_WORK_SCHEDULE_FRIDAY = SHOP_WORK_SCHEDULE_MONDAY.copy(day = WeekDay.FRIDAY)
    private val SHOP_WORK_SCHEDULE_SATURDAY = SHOP_WORK_SCHEDULE_MONDAY.copy(day = WeekDay.SATURDAY)
    private val SHOP_WORK_SCHEDULE_SATURDAY_MIDDAY = SHOP_WORK_SCHEDULE_MONDAY_MIDDAY.copy(day = WeekDay.SATURDAY)
    private val SHOP_WORK_SCHEDULE_SUNDAY = SHOP_WORK_SCHEDULE_MONDAY.copy(day = WeekDay.SUNDAY)
    private val SHOP_WORK_SCHEDULE_SUNDAY_MIDDAY = SHOP_WORK_SCHEDULE_MONDAY_MIDDAY.copy(day = WeekDay.SUNDAY)

    private val EMPTY_SCHEDULES = emptyList<ShopWorkSchedule>()
    private val ALL_SCHEDULES_SAME = listOf(
        SHOP_WORK_SCHEDULE_MONDAY,
        SHOP_WORK_SCHEDULE_TUESDAY,
        SHOP_WORK_SCHEDULE_WEDNESDAY,
        SHOP_WORK_SCHEDULE_THURSDAY,
        SHOP_WORK_SCHEDULE_FRIDAY,
        SHOP_WORK_SCHEDULE_SATURDAY,
        SHOP_WORK_SCHEDULE_SUNDAY
    )
    private val WEEKDAYS_AND_WEEKENDS_SCHEDULES_SAME = listOf(
        SHOP_WORK_SCHEDULE_MONDAY,
        SHOP_WORK_SCHEDULE_TUESDAY,
        SHOP_WORK_SCHEDULE_WEDNESDAY,
        SHOP_WORK_SCHEDULE_THURSDAY,
        SHOP_WORK_SCHEDULE_FRIDAY,
        SHOP_WORK_SCHEDULE_SATURDAY_MIDDAY,
        SHOP_WORK_SCHEDULE_SUNDAY_MIDDAY
    )
    private val DEFAULT_SCHEDULES = listOf(
        SHOP_WORK_SCHEDULE_MONDAY,
        SHOP_WORK_SCHEDULE_TUESDAY,
        SHOP_WORK_SCHEDULE_WEDNESDAY,
        SHOP_WORK_SCHEDULE_THURSDAY,
        SHOP_WORK_SCHEDULE_FRIDAY,
        SHOP_WORK_SCHEDULE_SATURDAY,
        SHOP_WORK_SCHEDULE_SUNDAY_MIDDAY
    )

    val SHOP_INFO_EMPTY_SCHEDULES = ShopInfo(
        id = ID,
        name = NAME,
        logo = LOGO,
        rating = RATING,
        currentWorkSchedule = SHOP_WORK_SCHEDULE_MONDAY,
        workScheduleList = EMPTY_SCHEDULES,
        brandColor = null,
        businessId = null,
        isShopOpenNow = true,
        operationalRating = null,
        tomorrowWorkSchedule = null,
        gradesPerAllTime = null,
        gradesPerThreeMonths = null,
        shopBrandName = null,
        logos = null,
        delivery = null,
        garsonWidgetSize = ShopCmsWidgetGarsonSize.BIG,
    )
    val SHOP_INFO_NULLABLE_SCHEDULES = SHOP_INFO_EMPTY_SCHEDULES.copy(
        workScheduleList = listOf(null, null),
    )
    val SHOP_INFO_ALL_SCHEDULES_SAME = SHOP_INFO_EMPTY_SCHEDULES.copy(
        workScheduleList = ALL_SCHEDULES_SAME
    )
    val SHOP_INFO_WEEKDAYS_AND_WEEKENDS_SCHEDULES_SAME = SHOP_INFO_EMPTY_SCHEDULES.copy(
        workScheduleList = WEEKDAYS_AND_WEEKENDS_SCHEDULES_SAME
    )
    val SHOP_INFO_DEFAULT_SCHEDULES = SHOP_INFO_EMPTY_SCHEDULES.copy(
        workScheduleList = DEFAULT_SCHEDULES
    )
    val SHOP_INFO_SCHEDULES_WHOLE_DAY = SHOP_INFO_EMPTY_SCHEDULES.copy(
        workScheduleList = DEFAULT_SCHEDULES.map {
            it.copy(
                from = ShopWorkScheduleTime(0, 0),
                to = ShopWorkScheduleTime(23, 59)
            )
        }
    )
    val SHOP_INFO_SHUFFLED_SCHEDULES = SHOP_INFO_DEFAULT_SCHEDULES.copy(
        workScheduleList = listOf(
            SHOP_WORK_SCHEDULE_TUESDAY,
            SHOP_WORK_SCHEDULE_FRIDAY,
            SHOP_WORK_SCHEDULE_SATURDAY,
            SHOP_WORK_SCHEDULE_SUNDAY_MIDDAY,
            SHOP_WORK_SCHEDULE_MONDAY_MIDDAY,
            SHOP_WORK_SCHEDULE_SATURDAY_MIDDAY,
        ),
    )

    private val SHOP_SCHEDULE_TIME_PERIOD_VO_ALL_SCHEDULES_SAME = ShopScheduleTimePeriodVo(
        name = DAILY,
        period = SHOP_SCHEDULE_EVERYDAY_FROM_TO
    )
    private val SHOP_SCHEDULE_TIME_PERIOD_VO_WEEKDAYS_SCHEDULES_SAME = ShopScheduleTimePeriodVo(
        name = WEEKDAYS,
        period = SHOP_SCHEDULE_EVERYDAY_PERIOD
    )
    private val SHOP_SCHEDULE_TIME_PERIOD_VO_WEEKENDS_SCHEDULES_SAME = ShopScheduleTimePeriodVo(
        name = WEEKENDS,
        period = SHOP_SCHEDULE_EVERYDAY_MIDDAY_PERIOD
    )
    private val SHOP_SCHEDULE_TIME_PERIOD_VO_MONDAY = ShopScheduleTimePeriodVo(
        name = MONDAY,
        period = SHOP_SCHEDULE_EVERYDAY_PERIOD
    )
    private val SHOP_SCHEDULE_TIME_PERIOD_VO_TUESDAY = SHOP_SCHEDULE_TIME_PERIOD_VO_MONDAY.copy(name = TUESDAY)
    private val SHOP_SCHEDULE_TIME_PERIOD_VO_WEDNESDAY = SHOP_SCHEDULE_TIME_PERIOD_VO_MONDAY.copy(name = WEDNESDAY)
    private val SHOP_SCHEDULE_TIME_PERIOD_VO_THURSDAY = SHOP_SCHEDULE_TIME_PERIOD_VO_MONDAY.copy(name = THURSDAY)
    private val SHOP_SCHEDULE_TIME_PERIOD_VO_FRIDAY = SHOP_SCHEDULE_TIME_PERIOD_VO_MONDAY.copy(name = FRIDAY)
    private val SHOP_SCHEDULE_TIME_PERIOD_VO_SATURDAY = SHOP_SCHEDULE_TIME_PERIOD_VO_MONDAY.copy(name = SATURDAY)
    private val SHOP_SCHEDULE_TIME_PERIOD_VO_SUNDAY = ShopScheduleTimePeriodVo(
        name = SUNDAY,
        period = SHOP_SCHEDULE_EVERYDAY_MIDDAY_PERIOD
    )

    val SHOP_SCHEDULE_VO_EMPTY_SCHEDULES = ShopScheduleVo(
        title = DIALOG_SHOP_SCHEDULE_TITLE,
        schedules = emptyList()
    )
    val SHOP_SCHEDULE_VO_ALL_SCHEDULES_SAME = SHOP_SCHEDULE_VO_EMPTY_SCHEDULES.copy(
        schedules = listOf(SHOP_SCHEDULE_TIME_PERIOD_VO_ALL_SCHEDULES_SAME)
    )
    val SHOP_SCHEDULE_VO_WEEKDAYS_AND_WEEKENDS_SCHEDULES_SAME = SHOP_SCHEDULE_VO_EMPTY_SCHEDULES.copy(
        schedules = listOf(
            SHOP_SCHEDULE_TIME_PERIOD_VO_WEEKDAYS_SCHEDULES_SAME,
            SHOP_SCHEDULE_TIME_PERIOD_VO_WEEKENDS_SCHEDULES_SAME
        )
    )
    val SHOP_SCHEDULE_VO_DEFAULT_SCHEDULES = SHOP_SCHEDULE_VO_EMPTY_SCHEDULES.copy(
        schedules = listOf(
            SHOP_SCHEDULE_TIME_PERIOD_VO_MONDAY,
            SHOP_SCHEDULE_TIME_PERIOD_VO_TUESDAY,
            SHOP_SCHEDULE_TIME_PERIOD_VO_WEDNESDAY,
            SHOP_SCHEDULE_TIME_PERIOD_VO_THURSDAY,
            SHOP_SCHEDULE_TIME_PERIOD_VO_FRIDAY,
            SHOP_SCHEDULE_TIME_PERIOD_VO_SATURDAY,
            SHOP_SCHEDULE_TIME_PERIOD_VO_SUNDAY
        )
    )
    val SHOP_SCHEDULE_VO_SCHEDULES_WHOLE_DAY = SHOP_SCHEDULE_VO_DEFAULT_SCHEDULES.copy(
        schedules = listOf(
            ShopScheduleTimePeriodVo(
                name = DAILY,
                period = WHOLE_DAY,
            )
        )
    )

    val SHOP_SCHEDULE_VO_SHUFFLED_SCHEDULES = SHOP_SCHEDULE_VO_DEFAULT_SCHEDULES.copy(
        schedules = listOf(
            SHOP_SCHEDULE_TIME_PERIOD_VO_MONDAY.copy(period = SHOP_SCHEDULE_EVERYDAY_MIDDAY_PERIOD),
            SHOP_SCHEDULE_TIME_PERIOD_VO_TUESDAY,
            SHOP_SCHEDULE_TIME_PERIOD_VO_FRIDAY,
            SHOP_SCHEDULE_TIME_PERIOD_VO_SATURDAY.copy(period = SHOP_SCHEDULE_EVERYDAY_MIDDAY_PERIOD),
            SHOP_SCHEDULE_TIME_PERIOD_VO_SUNDAY,
        )
    )
}
