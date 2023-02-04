package com.yandex.mobile.realty.ui.presenter

import com.yandex.mobile.realty.RobolectricTest
import com.yandex.mobile.realty.domain.model.common.Area
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * @author shpigun on 20.11.2020
 */
@RunWith(RobolectricTestRunner::class)
class AreaTest : RobolectricTest() {
    private val areaValue = 15.555

    @Test
    fun areaSquareMeter() {
        val area = Area(areaValue, Area.Unit.SQUARE_METER)
        assertEquals("15,6\u00a0м²", area.getDisplayString())
    }

    @Test
    fun areaAre() {
        val area = Area(areaValue, Area.Unit.ARE)
        assertEquals("15,6\u00a0сот.", area.getDisplayString())
    }

    @Test
    fun areaSquareKilometer() {
        val area = Area(areaValue, Area.Unit.SQUARE_KILOMETER)
        assertEquals("15,6\u00a0км²", area.getDisplayString())
    }

    @Test
    fun areaHectare() {
        val area = Area(areaValue, Area.Unit.HECTARE)
        assertEquals("15,55\u00a0га", area.getDisplayString())
    }
}
