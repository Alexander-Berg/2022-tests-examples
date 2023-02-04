package ru.yandex.realty.filters.commercial;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static java.lang.String.valueOf;
import static ru.yandex.realty.consts.Filters.COMMERCIAL;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Filters.UCHASTOK_KOMMERCHESKOGO_NAZNACHENIYA;
import static ru.yandex.realty.consts.Owners.KOPITSA;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.element.saleads.FiltersBlock.PRICE_FROM;
import static ru.yandex.realty.element.saleads.FiltersBlock.TO;
import static ru.yandex.realty.step.UrlSteps.PRICE_MAX_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.PRICE_MIN_URL_PARAM;
import static ru.yandex.realty.utils.UtilsWeb.getNormalPrice;

@DisplayName("Фильтры поиска по коммерческой недвижимости")
@Feature(FILTERS)
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
    @Category({Regression.class, Production.class})
    @Owner(KOPITSA)
    @DisplayName("Параметр цена «от»")
    public void shouldSeePriceMinInUrl() {
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(COMMERCIAL).open();
        String priceMin = valueOf(getNormalPrice());
        basePageSteps.onCommercialPage().filters().price().input(PRICE_FROM).sendKeys(priceMin);
        basePageSteps.onCommercialPage().filters().submitButton().click();
        urlSteps.queryParam(PRICE_MIN_URL_PARAM, priceMin).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KOPITSA)
    @DisplayName("Параметр цена «до»")
    public void shouldSeePriceMaxInUrl() {
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(COMMERCIAL).open();
        String priceMax = valueOf(getNormalPrice());
        basePageSteps.onCommercialPage().filters().price().input(TO).sendKeys(priceMax);
        basePageSteps.onCommercialPage().filters().submitButton().click();
        urlSteps.queryParam(PRICE_MAX_URL_PARAM, priceMax).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KOPITSA)
    @DisplayName("Параметр «Тип цены»")
    public void shouldSeePriceTypeInUrl() {
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(COMMERCIAL).open();
        basePageSteps.onCommercialPage().filters().checkButton("м²");
        basePageSteps.loaderWait();
        urlSteps.queryParam(PRICE_TYPE, "PER_METER").shouldNotDiffWithWebDriverUrl();
    }


    @Test
    @Category({Regression.class, Production.class})
    @Owner(KOPITSA)
    @DisplayName("Параметр «за сот.»")
    public void shouldSeePricePerAreFiltersInUrl() {
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(COMMERCIAL).path(UCHASTOK_KOMMERCHESKOGO_NAZNACHENIYA)
                .open();
        basePageSteps.onCommercialPage().filters().checkButton("за сот.");
        basePageSteps.loaderWait();
        urlSteps.queryParam(PRICE_TYPE, "PER_ARE").shouldNotDiffWithWebDriverUrl();
    }
}
