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
import ru.auto.tests.commons.util.Utils;
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.realty.consts.Filters.COMMERCIAL;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.OFIS;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Filters.UCHASTOK_KOMMERCHESKOGO_NAZNACHENIYA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.utils.UtilsWeb.getNormalArea;

@DisplayName("Фильтры поиска по коммерческой недвижимости")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class BaseFiltersAreaCommercialTest {

    private static final String AREA_FROM = "Площадь от";
    private static final String AREA_TO = "до";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps user;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Площадь от»")
    public void shouldSeeAreaFromInUrl() {
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(COMMERCIAL).path(OFIS).open();
        String areaFrom = String.valueOf(getNormalArea());
        user.onCommercialPage().filters().area().input(AREA_FROM).sendKeys(areaFrom);
        user.onCommercialPage().filters().submitButton().click();
        urlSteps.queryParam("areaMin", areaFrom).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Площадь до»")
    public void shouldSeeAreaToInUrl() {
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(COMMERCIAL).path(OFIS).open();
        String areaFrom = String.valueOf(getNormalArea());
        user.onCommercialPage().filters().area().input(AREA_TO).sendKeys(areaFrom);
        user.onCommercialPage().filters().submitButton().click();
        urlSteps.queryParam("areaMax", areaFrom).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Участок от»")
    public void shouldSeeLotAreaFromInUrl() {
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(COMMERCIAL).path(UCHASTOK_KOMMERCHESKOGO_NAZNACHENIYA)
                .open();
        String areaFrom = String.valueOf(Utils.getRandomShortInt());
        user.onCommercialPage().filters().lotArea().input(AREA_FROM).sendKeys(areaFrom);
        user.onCommercialPage().filters().submitButton().click();
        urlSteps.queryParam("lotAreaMin", areaFrom).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Участок до»")
    public void shouldSeeLotAreaToInUrl() {
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(COMMERCIAL).path(UCHASTOK_KOMMERCHESKOGO_NAZNACHENIYA)
                .open();
        String areaFrom = String.valueOf(Utils.getRandomShortInt());
        user.onCommercialPage().filters().lotArea().input(AREA_TO).sendKeys(areaFrom);
        user.onCommercialPage().filters().submitButton().click();
        urlSteps.queryParam("lotAreaMax", areaFrom).shouldNotDiffWithWebDriverUrl();
    }

}