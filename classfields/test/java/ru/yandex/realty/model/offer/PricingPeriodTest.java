package ru.yandex.realty.model.offer;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.common.util.collections.Cf;
import ru.yandex.realty.model.offer.PricingPeriod;

import java.util.List;

/**
 * @author aherman
 */
public class PricingPeriodTest {
    private List<PricingPeriod> periods;

    @Before
    public void setUp() throws Exception {
        periods = Cf.list(
                PricingPeriod.PER_HOUR,
                PricingPeriod.PER_DAY,
                PricingPeriod.PER_WEEK,
                PricingPeriod.PER_MONTH,
                PricingPeriod.PER_QUARTER,
                PricingPeriod.PER_YEAR
        );
    }

    @Test
    public void testConvert() throws Exception {
        Assert.assertEquals(24.0f, PricingPeriod.PER_DAY.convertFrom(1.0f, PricingPeriod.PER_HOUR), 0.000001f);
        Assert.assertEquals(168.0f, PricingPeriod.PER_WEEK.convertFrom(1.0f, PricingPeriod.PER_HOUR), 0.000001f);
        Assert.assertEquals(720.0f, PricingPeriod.PER_MONTH.convertFrom(1.0f, PricingPeriod.PER_HOUR), 0.000001f);
        Assert.assertEquals(30.0f, PricingPeriod.PER_MONTH.convertFrom(1.0f, PricingPeriod.PER_DAY), 0.000001f);
        Assert.assertEquals(90.0f, PricingPeriod.PER_QUARTER.convertFrom(1.0f, PricingPeriod.PER_DAY), 0.000001f);
        Assert.assertEquals(2160.0f, PricingPeriod.PER_QUARTER.convertFrom(1.0f, PricingPeriod.PER_HOUR), 0.000001f);
    }

    @Test
    public void testZero() throws Exception {
        for (PricingPeriod period1 : periods) {
            for (PricingPeriod period2 : periods) {
                Assert.assertEquals(period1 + " -> " + period2, 0.0f, period2.convertFrom(0.0f, period1), 0.000001f);
            }
        }
    }

    @Test
    public void testIdentity() throws Exception {
        for (PricingPeriod period : periods) {
            Assert.assertEquals(period + " -> " + period, 0.00000001f, period.convertFrom(0.00000001f, period), 0.0000000001f);
            Assert.assertEquals(period + " -> " + period, 1.0f, period.convertFrom(1.0f, period), 0.000001f);
            Assert.assertEquals(period + " -> " + period, 10.0f, period.convertFrom(10.0f, period), 0.000001f);
            Assert.assertEquals(period + " -> " + period, 123.0f, period.convertFrom(123.0f, period), 0.000001f);
            Assert.assertEquals(period + " -> " + period, 1000000000.0f, period.convertFrom(1000000000.0f, period), 0.000001f);
        }
    }
}
