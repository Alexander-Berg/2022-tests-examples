package com.yandex.mobile.realty.network.model.mapping

import com.yandex.mobile.realty.data.mapping.EmptyDescriptor
import com.yandex.mobile.realty.data.model.HeatMapInfoDto
import com.yandex.mobile.realty.domain.model.map.HeatMapType
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * @author rogovalex on 05.04.18.
 */
class HeatMapTypeDtoTest {

    @Test
    fun convertTransport() {
        val type = HeatMapInfoDto.TYPE_CONVERTER.map("transport", EmptyDescriptor)
        assertEquals(HeatMapType.TRANSPORT, type)
    }

    @Test
    fun convertInfrastructure() {
        val type = HeatMapInfoDto.TYPE_CONVERTER.map("infrastructure", EmptyDescriptor)
        assertEquals(HeatMapType.INFRASTRUCTURE, type)
    }

    @Test
    fun convertEcology() {
        val type = HeatMapInfoDto.TYPE_CONVERTER.map("ecology", EmptyDescriptor)
        assertEquals(HeatMapType.ECOLOGY, type)
    }

    @Test
    fun convertPriceSell() {
        val type = HeatMapInfoDto.TYPE_CONVERTER.map("price-sell", EmptyDescriptor)
        assertEquals(HeatMapType.PRICE_SELL, type)
    }

    @Test
    fun convertPriceRent() {
        val type = HeatMapInfoDto.TYPE_CONVERTER.map("price-rent", EmptyDescriptor)
        assertEquals(HeatMapType.PRICE_RENT, type)
    }

    @Test
    fun convertProfitability() {
        val type = HeatMapInfoDto.TYPE_CONVERTER.map("profitability", EmptyDescriptor)
        assertEquals(HeatMapType.PROFITABILITY, type)
    }

    @Test
    fun convertCarsharing() {
        val type = HeatMapInfoDto.TYPE_CONVERTER.map("carsharing", EmptyDescriptor)
        assertEquals(HeatMapType.CARSHARING, type)
    }
}
