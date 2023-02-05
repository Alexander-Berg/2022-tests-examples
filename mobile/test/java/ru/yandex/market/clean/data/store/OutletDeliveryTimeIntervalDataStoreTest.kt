package ru.yandex.market.clean.data.store

import org.assertj.core.api.Assertions
import org.junit.Test
import ru.yandex.market.clean.domain.OutletDeliveryTimeInterval

class OutletDeliveryTimeIntervalDataStoreTest {
    private val cache = OutletDeliveryTimeIntervalDataStore()

    @Test
    fun `Check if data store caches intervals correctly`() {

        val interval = OutletDeliveryTimeInterval(outletId, fromTime, toTime)
        cache.add(interval)

        val mapped = cache.get(outletId)

        Assertions.assertThat(mapped).extracting { it?.outletId }.isEqualTo(outletId)
        Assertions.assertThat(mapped).extracting { it?.fromTime }.isEqualTo(fromTime)
        Assertions.assertThat(mapped).extracting { it?.toTime }.isEqualTo(toTime)

        val nonExistingInterval = cache.get("undefine")
        Assertions.assertThat(nonExistingInterval).isNull()
    }


    companion object {
        const val outletId = "12345"
        const val fromTime = "10:11"
        const val toTime = "12:13"
    }
}