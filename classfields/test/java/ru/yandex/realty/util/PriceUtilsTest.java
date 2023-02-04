package ru.yandex.realty.util;

import org.junit.Test;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.realty.model.offer.AreaInfo;
import ru.yandex.realty.model.offer.AreaUnit;
import ru.yandex.realty.model.offer.PriceInfo;
import ru.yandex.realty.model.offer.PricingPeriod;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

/**
 * author: aherman
 */
public class PriceUtilsTest {
    @Test
    public void testConvertPriceUnit_Identity() throws Exception {
        PriceInfo expected = PriceInfo.create(Currency.RUR, 10f, PricingPeriod.WHOLE_LIFE, AreaUnit.WHOLE_OFFER);
        assertEquals(expected, PriceUtils.convertPriceUnit(expected, null, AreaUnit.WHOLE_OFFER));

        expected = PriceInfo.create(Currency.RUR, 20f, PricingPeriod.WHOLE_LIFE, AreaUnit.ARE);
        assertEquals(expected, PriceUtils.convertPriceUnit(expected, null, AreaUnit.ARE));
    }

    @Test
    public void testConvertPriceUnit_WholeToUnit() throws Exception {
        PriceInfo price = PriceInfo.create(Currency.RUR, 200f, PricingPeriod.WHOLE_LIFE, AreaUnit.WHOLE_OFFER);
        AreaInfo area = AreaInfo.create(AreaUnit.SQUARE_METER, 10f);

        PriceInfo expected = PriceInfo.create(Currency.RUR, 20f, PricingPeriod.WHOLE_LIFE, AreaUnit.SQUARE_METER);
        assertEquals(expected, PriceUtils.convertPriceUnit(price, area, AreaUnit.SQUARE_METER));
    }

    @Test
    public void testConvertPriceUnit_UnitToWhole() throws Exception {
        PriceInfo price = PriceInfo.create(Currency.RUR, 30f, PricingPeriod.WHOLE_LIFE, AreaUnit.ARE);
        AreaInfo area = AreaInfo.create(AreaUnit.ARE, 10f);

        PriceInfo expected = PriceInfo.create(Currency.RUR, 300f, PricingPeriod.WHOLE_LIFE, AreaUnit.WHOLE_OFFER);
        assertEquals(expected, PriceUtils.convertPriceUnit(price, area, AreaUnit.WHOLE_OFFER));
    }

    @Test
    public void testConvertPriceUnit_UnitToUnit() throws Exception {
        PriceInfo price = PriceInfo.create(Currency.RUR, 4f, PricingPeriod.WHOLE_LIFE, AreaUnit.SQUARE_METER);

        PriceInfo expected = PriceInfo.create(Currency.RUR, 400f, PricingPeriod.WHOLE_LIFE, AreaUnit.ARE);
        assertEquals(expected, PriceUtils.convertPriceUnit(price, null, AreaUnit.ARE));
    }

    @Test
    public void testConvertPriceUnit_UnitToUnit1() throws Exception {
        PriceInfo price = PriceInfo.create(Currency.RUR, 400f, PricingPeriod.WHOLE_LIFE, AreaUnit.ARE);

        PriceInfo expected = PriceInfo.create(Currency.RUR, 4f, PricingPeriod.WHOLE_LIFE, AreaUnit.SQUARE_METER);
        assertEquals(expected, PriceUtils.convertPriceUnit(price, null, AreaUnit.SQUARE_METER));
    }

    @Test
    public void testConvertPricePeriod_Identity() throws Exception {
        PriceInfo expected = PriceInfo.create(Currency.RUR, 10f, PricingPeriod.WHOLE_LIFE, AreaUnit.ARE);
        assertEquals(expected, PriceUtils.convertPricePeriod(expected, PricingPeriod.WHOLE_LIFE));

        expected = PriceInfo.create(Currency.RUR, 10f, PricingPeriod.PER_MONTH, AreaUnit.ARE);
        assertEquals(expected, PriceUtils.convertPricePeriod(expected, PricingPeriod.PER_MONTH));
    }

    @Test
    public void testConvertPricePeriod_WholeLife() throws Exception {
        try {
            PriceUtils.convertPricePeriod(PriceInfo.create(Currency.RUR, 10f, PricingPeriod.WHOLE_LIFE, AreaUnit.ARE),
                    PricingPeriod.PER_MONTH);
            fail("Conversion WHOLE_LIFE -> PER_* are forbidden");
        } catch (Exception e) {
        }

        try {
            PriceUtils.convertPricePeriod(PriceInfo.create(Currency.RUR, 10f, PricingPeriod.PER_MONTH, AreaUnit.ARE),
                    PricingPeriod.WHOLE_LIFE);
            fail("Conversion PER_* -> WHOLE_LIFE are forbidden");
        } catch (Exception e) {
        }
    }

    @Test
    public void testConvertPricePeriod_Unit() throws Exception {
        PriceInfo price = PriceInfo.create(Currency.RUR, 10f, PricingPeriod.PER_MONTH, AreaUnit.WHOLE_OFFER);
        PriceInfo expected = PriceInfo.create(Currency.RUR, 120f, PricingPeriod.PER_YEAR, AreaUnit.WHOLE_OFFER);

        assertEquals(expected, PriceUtils.convertPricePeriod(price, PricingPeriod.PER_YEAR));
    }

    @Test
    public void testConvertPricePeriod_Unit1() throws Exception {
        PriceInfo price = PriceInfo.create(Currency.RUR, 30f, PricingPeriod.PER_QUARTER, AreaUnit.WHOLE_OFFER);
        PriceInfo expected = PriceInfo.create(Currency.RUR, 10f, PricingPeriod.PER_MONTH, AreaUnit.WHOLE_OFFER);

        assertEquals(expected, PriceUtils.convertPricePeriod(price, PricingPeriod.PER_MONTH));
    }
}
