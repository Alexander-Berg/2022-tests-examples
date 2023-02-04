package ru.yandex.realty.filters.map.offers;

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
import static org.hamcrest.Matchers.hasItem;
import static ru.yandex.realty.consts.Filters.DOM;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Filters.UCHASTOK;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAPFILTERS;
import static ru.yandex.realty.element.saleads.FiltersBlock.AREA_MAX;
import static ru.yandex.realty.element.saleads.FiltersBlock.AREA_MIN;
import static ru.yandex.realty.element.saleads.FiltersBlock.TO;
import static ru.yandex.realty.step.BasePageSteps.urlParam;
import static ru.yandex.realty.utils.UtilsWeb.getNormalArea;

@DisplayName("Карта. Базовые фильтры поиска по объявлениям")
@Feature(MAPFILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ExtendedFiltersAreaTest {

    private static final String LOT_AREA_MAX = "lotAreaMax";
    private static final String LOT_AREA_MIN = "lotAreaMin";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр площадь «от»")
    public void shouldSeeAreaMinInUrl() {
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(KVARTIRA).path(KARTA).open();
        String areaMin = valueOf(getNormalArea());
        basePageSteps.onMapPage().openExtFilter();
        basePageSteps.onMapPage().extendFilters().byName("Площадь").input("от").sendKeys(areaMin + Keys.ENTER);
        basePageSteps.loaderWait();
        urlSteps.queryParam(AREA_MIN, areaMin).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр площадь «до»")
    public void shouldSeeAreaMaxInUrl() {
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(KVARTIRA).path(KARTA).open();
        String areaMax = valueOf(getNormalArea());
        basePageSteps.onMapPage().openExtFilter();
        basePageSteps.onMapPage().extendFilters().byName("Площадь").input("до").sendKeys(areaMax + Keys.ENTER);
        basePageSteps.loaderWait();
        urlSteps.queryParam(AREA_MAX, areaMax).shouldNotDiffWithWebDriverUrl();
    }


    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Дом от»")
    public void shouldSeeHouseAreaMinInLog() {
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(DOM).path(KARTA).open();
        String areaMin = valueOf(getNormalArea());
        basePageSteps.onMapPage().filters().area().input("Дом от").sendKeys(areaMin + Keys.ENTER);
        basePageSteps.loaderWait();
        urlSteps.queryParam(AREA_MIN, areaMin).shouldNotDiffWithWebDriverUrl();
        basePageSteps.shouldSeeRequestInBrowser(hasItem(urlParam(AREA_MIN, areaMin)));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Дом до»")
    public void shouldSeeHouseAreaMaxInLog() {
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(DOM).path(KARTA).open();
        String areaMax = valueOf(getNormalArea());
        basePageSteps.onMapPage().filters().area().input(TO).sendKeys(areaMax + Keys.ENTER);
        basePageSteps.loaderWait();
        urlSteps.queryParam(AREA_MAX, areaMax).shouldNotDiffWithWebDriverUrl();
        basePageSteps.shouldSeeRequestInBrowser(hasItem(urlParam(AREA_MAX, areaMax)));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Участок от»")
    public void shouldSeeLotAreaMinInLog() {
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(UCHASTOK).path(KARTA).open();
        String areaMin = valueOf(getNormalArea());
        basePageSteps.onMapPage().filters().input("Площадь от").sendKeys(areaMin + Keys.ENTER);
        basePageSteps.loaderWait();
        urlSteps.queryParam(LOT_AREA_MIN, areaMin).shouldNotDiffWithWebDriverUrl();
        basePageSteps.shouldSeeRequestInBrowser(hasItem(urlParam(LOT_AREA_MIN, areaMin)));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Участок до»")
    public void shouldSeeLotAreaMaxInLog() {
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(UCHASTOK).path(KARTA).open();
        String areaMax = valueOf(getNormalArea());
        basePageSteps.onMapPage().filters().input(TO).sendKeys(areaMax + Keys.ENTER);
        basePageSteps.loaderWait();
        urlSteps.queryParam(LOT_AREA_MAX, areaMax).shouldNotDiffWithWebDriverUrl();
        basePageSteps.shouldSeeRequestInBrowser(hasItem(urlParam(LOT_AREA_MAX, areaMax)));
    }
}
