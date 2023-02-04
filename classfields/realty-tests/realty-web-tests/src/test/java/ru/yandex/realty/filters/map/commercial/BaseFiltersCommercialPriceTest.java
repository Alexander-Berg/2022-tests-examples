package ru.yandex.realty.filters.map.commercial;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.Keys;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static java.lang.String.valueOf;
import static ru.yandex.realty.consts.Filters.COMMERCIAL;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Filters.UCHASTOK_KOMMERCHESKOGO_NAZNACHENIYA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAPFILTERS;
import static ru.yandex.realty.element.saleads.FiltersBlock.PRICE_FROM;
import static ru.yandex.realty.element.saleads.FiltersBlock.TO;
import static ru.yandex.realty.step.UrlSteps.PRICE_MAX_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.PRICE_MIN_URL_PARAM;
import static ru.yandex.realty.utils.UtilsWeb.getNormalPrice;

@DisplayName("Карта. Фильтры поиска по коммерческой недвижимости")
@Feature(MAPFILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class BaseFiltersCommercialPriceTest {

    public static final String PRICE_TYPE = "priceType";
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр цена «от»")
    public void shouldSeePriceMinInUrl() {
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(COMMERCIAL).path(KARTA).open();
        String priceMin = valueOf(getNormalPrice());
        basePageSteps.onMapPage().filters().price().input(PRICE_FROM).sendKeys(priceMin + Keys.ENTER);
        basePageSteps.loaderWait();
        urlSteps.queryParam(PRICE_MIN_URL_PARAM, priceMin).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр цена «до»")
    public void shouldSeePriceMaxInUrl() {
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(COMMERCIAL).path(KARTA).open();
        String priceMax = valueOf(getNormalPrice());
        basePageSteps.onMapPage().filters().price().input(TO).sendKeys(priceMax + Keys.ENTER);
        basePageSteps.loaderWait();
        urlSteps.queryParam(PRICE_MAX_URL_PARAM, priceMax).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Тип цены»")
    public void shouldSeePriceTypeInUrl() {
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(COMMERCIAL).path(KARTA).open();
        basePageSteps.onMapPage().filters().checkButton("м²");
        basePageSteps.loaderWait();
        urlSteps.queryParam(PRICE_TYPE, "PER_METER").shouldNotDiffWithWebDriverUrl();
    }


    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «за сот.»")
    public void shouldSeePricePerAreFiltersInUrl() {
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(COMMERCIAL).path(UCHASTOK_KOMMERCHESKOGO_NAZNACHENIYA)
                .path(KARTA).open();
        basePageSteps.onMapPage().filters().checkButton("за сот.");
        basePageSteps.loaderWait();
        urlSteps.queryParam(PRICE_TYPE, "PER_ARE").shouldNotDiffWithWebDriverUrl();
    }
}
