package com.yandex.mobile.realty.network.model.mapping

import com.yandex.mobile.realty.data.mapping.EmptyDescriptor
import com.yandex.mobile.realty.data.model.search.FilterConverters
import com.yandex.mobile.realty.domain.model.map.HeatMapType
import com.yandex.mobile.realty.domain.model.map.LayerType
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * @author rogovalex on 05.04.18.
 */
class LayerTypeTest {

    @Test
    fun valueOfTransport() {
        val type = LayerType.valueOf(HeatMapType.TRANSPORT)
        assertEquals(LayerType.TRANSPORT, type)
    }

    @Test
    fun valueOfInfrastructure() {
        val type = LayerType.valueOf(HeatMapType.INFRASTRUCTURE)
        assertEquals(LayerType.INFRASTRUCTURE, type)
    }

    @Test
    fun valueOfEcology() {
        val type = LayerType.valueOf(HeatMapType.ECOLOGY)
        assertEquals(LayerType.ECOLOGY, type)
    }

    @Test
    fun valueOfPriceSell() {
        val type = LayerType.valueOf(HeatMapType.PRICE_SELL)
        assertEquals(LayerType.PRICE_SELL, type)
    }

    @Test
    fun valueOfPriceRent() {
        val type = LayerType.valueOf(HeatMapType.PRICE_RENT)
        assertEquals(LayerType.PRICE_RENT, type)
    }

    @Test
    fun valueOfProfitability() {
        val type = LayerType.valueOf(HeatMapType.PROFITABILITY)
        assertEquals(LayerType.PROFITABILITY, type)
    }

    @Test
    fun valueOfCarsharing() {
        val type = LayerType.valueOf(HeatMapType.CARSHARING)
        assertEquals(LayerType.CARSHARING, type)
    }

    @Test
    fun convertToTransport() {
        val type = FilterConverters.LAYER_TYPE_CONVERTER.map("transport", EmptyDescriptor)
        assertEquals(LayerType.TRANSPORT, type)
    }

    @Test
    fun convertToInfrastructure() {
        val type = FilterConverters.LAYER_TYPE_CONVERTER.map("infrastructure", EmptyDescriptor)
        assertEquals(LayerType.INFRASTRUCTURE, type)
    }

    @Test
    fun convertToEcology() {
        val type = FilterConverters.LAYER_TYPE_CONVERTER.map("ecology", EmptyDescriptor)
        assertEquals(LayerType.ECOLOGY, type)
    }

    @Test
    fun convertToEducation() {
        val type = FilterConverters.LAYER_TYPE_CONVERTER.map("education", EmptyDescriptor)
        assertEquals(LayerType.EDUCATION, type)
    }

    @Test
    fun convertToProfitability() {
        val type = FilterConverters.LAYER_TYPE_CONVERTER.map("profitability", EmptyDescriptor)
        assertEquals(LayerType.PROFITABILITY, type)
    }

    @Test
    fun convertToPriceSell() {
        val type = FilterConverters.LAYER_TYPE_CONVERTER.map("price-sell", EmptyDescriptor)
        assertEquals(LayerType.PRICE_SELL, type)
    }

    @Test
    fun convertToPriceRent() {
        val type = FilterConverters.LAYER_TYPE_CONVERTER.map("price-rent", EmptyDescriptor)
        assertEquals(LayerType.PRICE_RENT, type)
    }

    @Test
    fun convertToCarsharing() {
        val type = FilterConverters.LAYER_TYPE_CONVERTER.map("carsharing", EmptyDescriptor)
        assertEquals(LayerType.CARSHARING, type)
    }
}
