package ru.yandex.market.clean.data.mapper

import org.assertj.core.api.Assertions
import org.junit.Test
import org.mockito.kotlin.mock
import ru.yandex.market.checkout.data.mapper.LocalTimeMapper
import ru.yandex.market.common.LocalTime
import ru.yandex.market.clean.data.fapi.dto.FrontApiShopWorkScheduleDto
import ru.yandex.market.clean.data.fapi.dto.FrontApiShopWorkTimeDto
import ru.yandex.market.clean.domain.model.order.delivery.DeliveryTimeInterval

class DeliveryTimeIntervalMapperTest {

    private val localTimeMapper = mock<LocalTimeMapper>()
    private val mapper = DeliveryTimeIntervalMapper(localTimeMapper)

    @Test
    fun `Test map empty FrontApiShopWorkScheduleDto to DeliveryTimeInterval`() {
        var dto: FrontApiShopWorkScheduleDto? = null
        var result = mapper.map(dto)
        Assertions.assertThat(result).isNull()

        dto = FrontApiShopWorkScheduleDto(null, null, null)
        //use min time for shop start and max time for shop close
        val defaultResult = DeliveryTimeInterval("", null, LocalTime.midnight(), LocalTime.dayEnd(), null)
        result = mapper.map(dto)
        Assertions.assertThat(result?.equalsIgnoreIds(defaultResult)).isTrue()
    }

    @Test
    fun `Test map FrontApiShopWorkScheduleDto to DeliveryTimeInterval`() {
        val fromHour = 8
        val fromMinute = 35
        val toHour = 18
        val toMinute = 45
        val day = 2
        val dto = FrontApiShopWorkScheduleDto(
            from = FrontApiShopWorkTimeDto(fromHour, fromMinute),
            to = FrontApiShopWorkTimeDto(toHour, toMinute),
            day = day,
        )
        val result = mapper.map(dto)
        val expectedResult = DeliveryTimeInterval(
            id = "",
            from = LocalTime.builder().hours(fromHour).minutes(fromMinute).seconds(0).build(),
            to = LocalTime.builder().hours(toHour).minutes(toMinute).seconds(0).build(),
            day = day,
            price = null,
        )
        Assertions.assertThat(result?.equalsIgnoreIds(expectedResult)).isTrue()
    }

}
