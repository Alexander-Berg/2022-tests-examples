package ru.yandex.market.clean.domain.model.retail

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class EatsRetailPathTest {

    @Test
    fun `should create cart path correct with shopId in query parameter`() {
        val expected = "market/cart?shopId=1"
        val actual = EatsRetailPath.Cart("1").value
        assertThat(expected).isEqualTo(actual)
    }
}
