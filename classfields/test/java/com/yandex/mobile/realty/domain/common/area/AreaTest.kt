package com.yandex.mobile.realty.domain.common.area

import com.yandex.mobile.realty.domain.model.common.Area
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * @author rogovalex on 12.08.2021.
 */
class AreaTest {

    @Test
    fun valueOfOneSquareMeter() {
        assertEquals(Area(1.0, Area.Unit.SQUARE_METER), Area.valueOf(1.0))
    }

    @Test
    fun valueOfDozensSquareMeters() {
        assertEquals(Area(99.0, Area.Unit.SQUARE_METER), Area.valueOf(99.0))
    }

    @Test
    fun valueOfOneHundredSquareMeters() {
        assertEquals(Area(1.0, Area.Unit.ARE), Area.valueOf(100.0))
    }

    @Test
    fun valueOfDozensHundredsSquareMeters() {
        assertEquals(Area(99.0, Area.Unit.ARE), Area.valueOf(9900.0))
    }

    @Test
    fun valueOfTenThousandsSquareMeters() {
        assertEquals(Area(1.0, Area.Unit.HECTARE), Area.valueOf(10_000.0))
    }

    @Test
    fun valueOfDozensThousandsSquareMeters() {
        assertEquals(Area(99.0, Area.Unit.HECTARE), Area.valueOf(990_000.0))
    }

    @Test
    fun valueOfOneMillionSquareMeters() {
        assertEquals(Area(1.0, Area.Unit.SQUARE_KILOMETER), Area.valueOf(1_000_000.0))
    }

    @Test
    fun valueOfMillionsSquareMeters() {
        assertEquals(Area(99.0, Area.Unit.SQUARE_KILOMETER), Area.valueOf(99_000_000.0))
    }
}
