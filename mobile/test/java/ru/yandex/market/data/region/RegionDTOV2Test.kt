package ru.yandex.market.data.region

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

import ru.yandex.market.domain.models.region.RegionType

class RegionDTOV2Test {

    @Test
    fun `Country region return self id as country id`() {
        val countryId = RegionDtoV2.testBuilder()
            .id(42)
            .type(RegionType.COUNTRY)
            .build()
            .countryId

        assertThat(countryId).extracting { it.asLong }.isEqualTo(42L)
    }

    @Test
    fun `Region return country id if present`() {
        val country = RegionDtoV2.testBuilder()
            .id(42)
            .type(RegionType.COUNTRY)
            .build()
        val countryId = RegionDtoV2.testBuilder()
            .id(0)
            .country(country)
            .build()
            .countryId

        assertThat(countryId).extracting { it.asLong }.isEqualTo(42L)
    }

    @Test
    fun `Region return country id from parent when country is not present`() {
        val parent = RegionDtoV2.testBuilder()
            .id(42)
            .type(RegionType.COUNTRY)
            .build()
        val countryId = RegionDtoV2.testBuilder()
            .id(0)
            .parent(parent)
            .build()
            .countryId

        assertThat(countryId).extracting { it.asLong }.isEqualTo(42L)
    }
}