package ru.yandex.market.clean.data.mapper

import org.assertj.core.api.Assertions
import org.junit.Test
import ru.yandex.market.clean.data.model.dto.OutletDeliveryTimeIntervalDto

class OutletDeliveryTimeIntervalMapperTest {

    private val mapper = OutletDeliveryTimeIntervalMapper()

    @Test
    fun `Check if delivery time interval dto is mapped correctly`() {
        val dto = OutletDeliveryTimeIntervalDto(outletId, fromTime, toTime)

        val mapped = mapper.map(dto)

        Assertions.assertThat(mapped).extracting { it.outletId }.isEqualTo(outletId.toString())
        Assertions.assertThat(mapped).extracting { it.fromTime }.isEqualTo(fromTime)
        Assertions.assertThat(mapped).extracting { it.toTime }.isEqualTo(toTime)
    }


    companion object {
        const val outletId = 123456L
        const val fromTime = "11:12"
        const val toTime = "13:14"
    }
}