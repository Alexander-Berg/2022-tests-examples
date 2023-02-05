package ru.yandex.market.clean.data.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.market.clean.data.model.dto.CartItemSnapshotDto
import ru.yandex.market.clean.domain.model.cartItemSnapshotTestInstance

class CartItemSnapshotDtoMapperTest {

    private val mapper = CartItemSnapshotDtoMapper()

    @Test
    fun `Check if all fields mapped to corresponding dto fields`() {
        val snapshot = cartItemSnapshotTestInstance()
        val expectedResult = CartItemSnapshotDto(
            persistentOfferId = snapshot.persistentOfferId,
            modelId = snapshot.modelId?.toString(),
            categoryId = snapshot.categoryId?.toString(),
            count = snapshot.count,
            stockKeepingUnitId = snapshot.stockKeepingUnitId,
            isPriceDropPromoEnabled = snapshot.isPriceDropPromoEnabled
        )

        assertThat(mapper.map(snapshot)).isEqualTo(expectedResult)
    }
}