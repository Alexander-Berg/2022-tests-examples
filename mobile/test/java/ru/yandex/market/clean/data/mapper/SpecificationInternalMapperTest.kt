package ru.yandex.market.clean.data.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.market.net.sku.fapi.dto.specs.specificationInternalDtoTestInstance

class SpecificationInternalMapperTest {
    val mapper = SpecificationInternalMapper()

    @Test
    fun `check internals`() {
        val spec = specificationInternalDtoTestInstance()
        assertThat(mapper.map(listOf(spec)).internals?.firstOrNull()).isSameAs(spec.value)
    }
}