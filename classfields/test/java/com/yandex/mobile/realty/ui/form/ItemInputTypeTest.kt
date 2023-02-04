package com.yandex.mobile.realty.ui.form

import com.yandex.mobile.realty.ui.form.adapter.adapteritems.ItemInputType
import com.yandex.mobile.realty.utils.setDefaultLocale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.*

/**
 * @author andrey-bgm on 21/12/2020.
 */
class ItemInputTypeTest {

    private val defaultLocale = Locale.getDefault()

    @Before
    fun setUp() {
        setDefaultLocale()
    }

    @After
    fun cleanUp() {
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun testPhoneInputType() {
        val phoneType = ItemInputType.PhoneType

        assertEquals("+7 (987) 123-45-67", phoneType.formatValue("+79871234567"))
        assertEquals("+7 (987) 123-45-6", phoneType.formatValue("+7987123456"))
        assertEquals("+7 (987) 123-45", phoneType.formatValue("+798712345"))
        assertEquals("+7 (987) 123-4", phoneType.formatValue("+79871234"))
        assertEquals("+7 (987) 123", phoneType.formatValue("+7987123"))
        assertEquals("+7 (987) 12", phoneType.formatValue("+798712"))
        assertEquals("+7 (987) 1", phoneType.formatValue("+79871"))
        assertEquals("+7 (987)", phoneType.formatValue("+7987"))
        assertEquals("+7 (98", phoneType.formatValue("+798"))
        assertEquals("+7 (9", phoneType.formatValue("+79"))
        assertEquals("+7", phoneType.formatValue("+7"))
        assertEquals("", phoneType.formatValue(""))

        assertEquals("79871234567", phoneType.removeFormattingFromText("+7 (987) 123-45-67"))

        assertEquals("+", phoneType.parseInput(""))
        assertEquals("+7", phoneType.parseInput("7"))
        assertEquals("+7", phoneType.parseInput("8"))
        assertEquals("+79", phoneType.parseInput("9"))
        assertEquals("+79", phoneType.parseInput("79"))
    }

    @Test
    fun testPriceInKopecksType() {
        val priceType = ItemInputType.PriceInKopecksType

        assertEquals("58\u00A0034,97", priceType.formatValue(5_803_497L))
        assertEquals("0", priceType.formatValue(0L))
        assertEquals("0,1", priceType.formatValue(10L))
        assertEquals("0,59", priceType.formatValue(59L))

        assertEquals("5803497", priceType.removeFormattingFromText("58\u00A0034,97"))
        assertEquals("5800", priceType.removeFormattingFromText("58"))
        assertEquals("55580", priceType.removeFormattingFromText("555,8"))
        assertEquals("", priceType.removeFormattingFromText(""))
        assertEquals("0", priceType.removeFormattingFromText("0"))
        assertEquals("0", priceType.removeFormattingFromText("000,00"))
        assertEquals("1000", priceType.removeFormattingFromText("010,00"))
        assertEquals("10", priceType.removeFormattingFromText("000,1"))
        assertEquals("10", priceType.removeFormattingFromText("0,10"))
        assertEquals("13", priceType.removeFormattingFromText("0,13"))
        assertEquals("1", priceType.removeFormattingFromText("0,01"))

        assertEquals("5", priceType.formatEditingValue(500L, "5"))
        assertEquals("5,", priceType.formatEditingValue(500L, "5,"))
        assertEquals("5,0", priceType.formatEditingValue(500L, "5,0"))
        assertEquals("5,00", priceType.formatEditingValue(500L, "5,00"))
        assertEquals("5,01", priceType.formatEditingValue(501L, "5,01"))
        assertEquals("58\u00A0034,01", priceType.formatEditingValue(5_803_401L, "58034,01"))
    }
}
