package ru.yandex.realty.search.common.request.domain;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.realty.context.ProviderAdapter;
import ru.yandex.realty.model.metro.MetroTransport;
import ru.yandex.realty.model.locale.RealtyLocale;
import ru.yandex.realty.model.offer.AreaInfo;
import ru.yandex.realty.model.offer.AreaUnit;
import ru.yandex.realty.model.offer.PriceInfo;
import ru.yandex.realty.model.offer.PricingPeriod;
import ru.yandex.realty.model.request.PriceType;
import ru.yandex.realty.storage.CurrencyStorage;
import ru.yandex.realty.util.PriceUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author nstaroverova
 */
@PrepareForTest(PriceUtils.class)
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("jdk.internal.reflect.*")
public class SerializeConfigTest {

    private final CurrencyStorage currencyStorage = mock(CurrencyStorage.class);
    private final Currency currency = Currency.RUR;
    private final Currency requestCurrency = Currency.RUR;
    private final PriceType priceType = PriceType.PER_SQUARE;
    private final MetroTransport metroTransport = null;
    private final RealtyLocale locale = RealtyLocale.RU;
    private final Currency regionDefaultCurrency = Currency.RUR;
    private final PricingPeriod pricingPeriod = PricingPeriod.WHOLE_LIFE;

    private SerializeConfig serializeConfig;

    @Before
    public void setUp() {
        when(currencyStorage.convert(any(PriceInfo.class), any(Currency.class))).then(new Answer<PriceInfo>() {
            @Override
            public PriceInfo answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                return (PriceInfo) args[0];
            }
        });
        PowerMockito.mockStatic(PriceUtils.class);

        serializeConfig = new SerializeConfig(
                ProviderAdapter.create(currencyStorage), currency, priceType, metroTransport,
                locale, regionDefaultCurrency, pricingPeriod
        );
    }

    @Test
    public void testGetPricePartInfoReturnsPriceBySquartMeter() {
        PriceInfo priceInfo = PriceInfo.createUnsafe(Currency.RUR, 6, PricingPeriod.WHOLE_LIFE, AreaUnit.WHOLE_OFFER);
        AreaInfo areaInfo = AreaInfo.create(AreaUnit.SQUARE_METER, 2.0f);
        AreaUnit desiredUnit = AreaUnit.SQUARE_METER;
        Currency desiredCurrency = Currency.RUR;
        PriceInfo convertUtilsResult = PriceInfo.createUnsafe(Currency.RUR, 3, PricingPeriod.WHOLE_LIFE, AreaUnit.SQUARE_METER);
        when(PriceUtils.convertPriceUnit(priceInfo, areaInfo, desiredUnit)).thenReturn(convertUtilsResult);

        PriceInfo priceInfoPerPart = serializeConfig.getPricePartInfo(priceInfo, areaInfo, desiredUnit, desiredCurrency);

        assertEquals(convertUtilsResult.getMoney().getValue(), priceInfoPerPart.getMoney().getValue(), 0.0);
    }

    @Test
    public void testGetPricePartInfoReturnsNullIfAreaInfoIsNull() {
        PriceInfo priceInfo = PriceInfo.createUnsafe(Currency.RUR, 6, PricingPeriod.WHOLE_LIFE, AreaUnit.WHOLE_OFFER);
        AreaInfo areaInfo = null;
        AreaUnit desiredUnit = AreaUnit.SQUARE_METER;
        Currency desiredCurrency = Currency.RUR;
        when(PriceUtils.convertPriceUnit(priceInfo, areaInfo, desiredUnit)).thenReturn(null);

        PriceInfo priceInfoPerPart = serializeConfig.getPricePartInfo(priceInfo, areaInfo, desiredUnit, desiredCurrency);

        assertNull(priceInfoPerPart);
    }

}
