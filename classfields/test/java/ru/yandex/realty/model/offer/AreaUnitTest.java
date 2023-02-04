package ru.yandex.realty.model.offer;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.common.util.collections.Cf;

import java.util.List;

/**
 * @author aherman
 */
public class AreaUnitTest {
    private List<AreaUnit> units;

    @Before
    public void setUp() throws Exception {
        units = Cf.list(AreaUnit.SQUARE_KILOMETER, AreaUnit.ARE, AreaUnit.HECTARE, AreaUnit.SQUARE_KILOMETER);
    }

    @Test
    public void testConversion() throws Exception {
        Assert.assertEquals(1.0f, AreaUnit.ARE.convertFrom(100, AreaUnit.SQUARE_METER), 0.000001f);
        Assert.assertEquals(0.01f, AreaUnit.ARE.convertFrom(1, AreaUnit.SQUARE_METER), 0.000001f);

        Assert.assertEquals(100.0f, AreaUnit.SQUARE_METER.convertFrom(1, AreaUnit.ARE), 0.000001f);
        Assert.assertEquals(1.0f, AreaUnit.SQUARE_METER.convertFrom(0.01f, AreaUnit.ARE), 0.000001f);
    }

    @Test
    public void testZero() throws Exception {
        for (AreaUnit unit1 : units) {
            for (AreaUnit unit2 : units) {
                Assert.assertEquals(unit1 + " -> " + unit2, 0.0f, unit2.convertFrom(0.0f, unit1), 0.000001f);
            }
        }
    }

    @Test
    public void testIdentity() throws Exception {
        for (AreaUnit unit : units) {
            Assert.assertEquals(unit + " -> " + unit, 0.00000001f, unit.convertFrom(0.00000001f, unit), 0.00000000001f);
            Assert.assertEquals(unit + " -> " + unit, 1.0f, unit.convertFrom(1.0f, unit), 0.000001f);
            Assert.assertEquals(unit + " -> " + unit, 10.0f, unit.convertFrom(10.0f, unit), 0.000001f);
            Assert.assertEquals(unit + " -> " + unit, 123.0f, unit.convertFrom(123.0f, unit), 0.000001f);
            Assert.assertEquals(unit + " -> " + unit, 1000000000.0f, unit.convertFrom(1000000000.0f, unit), 0.000001f);
        }
    }
}
