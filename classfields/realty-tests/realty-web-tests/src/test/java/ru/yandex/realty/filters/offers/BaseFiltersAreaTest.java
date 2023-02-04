package ru.yandex.realty.filters.offers;

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
import static org.hamcrest.Matchers.hasItem;
import static ru.yandex.realty.consts.Filters.DOM;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Filters.UCHASTOK;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.element.saleads.FiltersBlock.AREA_FROM;
import static ru.yandex.realty.element.saleads.FiltersBlock.AREA_MAX;
import static ru.yandex.realty.element.saleads.FiltersBlock.AREA_MIN;
import static ru.yandex.realty.element.saleads.FiltersBlock.TO;
import static ru.yandex.realty.step.BasePageSteps.urlParam;
import static ru.yandex.realty.utils.UtilsWeb.getNormalArea;

@DisplayName("Базовые фильтры поиска по объявлениям")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class BaseFiltersAreaTest {

    public static final String LOT_AREA_MAX = "lotAreaMax";
    public static final String LOT_AREA_MIN = "lotAreaMin";
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр площадь «от»")
    public void shouldSeeAreaMinInUrl() {
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(KVARTIRA).open();
        String areaMin = valueOf(getNormalArea());
        basePageSteps.onOffersSearchPage().filters().area().input(AREA_FROM).sendKeys(areaMin);
        basePageSteps.onOffersSearchPage().filters().submitButton().click();
        urlSteps.queryParam(AREA_MIN, areaMin).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр площадь «до»")
    public void shouldSeeAreaMaxInUrl() {
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(KVARTIRA).open();
        String areaMax = valueOf(getNormalArea());
        basePageSteps.onOffersSearchPage().filters().area().input(TO).sendKeys(areaMax);
        basePageSteps.onOffersSearchPage().filters().submitButton().click();
        urlSteps.queryParam(AREA_MAX, areaMax).shouldNotDiffWithWebDriverUrl();
    }


    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Дом от»")
    public void shouldSeeHouseAreaMinInLog() {
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(DOM).open();
        String areaMin = valueOf(getNormalArea());
        basePageSteps.onOffersSearchPage().filters().area().input("Дом от").sendKeys(areaMin);
        basePageSteps.onOffersSearchPage().filters().submitButton().click();
        urlSteps.queryParam(AREA_MIN, areaMin).shouldNotDiffWithWebDriverUrl();
        basePageSteps.shouldSeeRequestInBrowser(hasItem(urlParam(AREA_MIN, areaMin)));
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Дом до»")
    public void shouldSeeHouseAreaMaxInLog() {
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(DOM).open();
        String areaMax = valueOf(getNormalArea());
        basePageSteps.onOffersSearchPage().filters().area().input(TO).sendKeys(areaMax);
        basePageSteps.onOffersSearchPage().filters().submitButton().click();
        urlSteps.queryParam(AREA_MAX, areaMax).shouldNotDiffWithWebDriverUrl();
        basePageSteps.shouldSeeRequestInBrowser(hasItem(urlParam(AREA_MAX, areaMax)));
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Участок от»")
    public void shouldSeeLotAreaMinInLog() {
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(UCHASTOK).open();
        String areaMin = valueOf(getNormalArea());
        basePageSteps.onOffersSearchPage().filters().input("Площадь от").sendKeys(areaMin);
        basePageSteps.onOffersSearchPage().filters().submitButton().click();
        urlSteps.queryParam(LOT_AREA_MIN, areaMin).shouldNotDiffWithWebDriverUrl();
        basePageSteps.shouldSeeRequestInBrowser(hasItem(urlParam(LOT_AREA_MIN, areaMin)));
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Участок до»")
    public void shouldSeeLotAreaMaxInLog() {
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(UCHASTOK).open();
        String areaMax = valueOf(getNormalArea());
        basePageSteps.onOffersSearchPage().filters().input(TO).sendKeys(areaMax);
        basePageSteps.onOffersSearchPage().filters().submitButton().click();
        urlSteps.queryParam(LOT_AREA_MAX, areaMax).shouldNotDiffWithWebDriverUrl();
        basePageSteps.shouldSeeRequestInBrowser(hasItem(urlParam(LOT_AREA_MAX, areaMax)));
    }
}
