package ru.yandex.realty.filters.map.offers;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.Keys;
import ru.auto.tests.commons.util.Utils;
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.Matchers.hasItem;
import static ru.yandex.realty.consts.Filters.DOM;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAPFILTERS;
import static ru.yandex.realty.step.BasePageSteps.urlParam;

@DisplayName("Карта. Расширенные фильтры поиска по объявлениям.")
@Feature(MAPFILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ExtendedFiltersBuyHouseTest {

    private static final String LOT_AREA_MIN = "lotAreaMin";
    private static final String LOT_AREA_MAX = "lotAreaMax";
    private static final String AREA_LOT = "Площадь участка";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(DOM).path(KARTA).open();
        basePageSteps.onMapPage().openExtFilter();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Участок от»")
    public void shouldSeeLotAreaMinInUrl() {
        String areaFrom = String.valueOf(Utils.getRandomShortInt());
        basePageSteps.onMapPage().extendFilters().byExactName(AREA_LOT).input("от").sendKeys(areaFrom + Keys.ENTER);
        basePageSteps.loaderWait();
        urlSteps.queryParam(LOT_AREA_MIN, areaFrom).shouldNotDiffWithWebDriverUrl();
        basePageSteps.shouldSeeRequestInBrowser(hasItem(urlParam(LOT_AREA_MIN, areaFrom)));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Участок до»")
    public void shouldSeeLotAreaMaxInUrl() {
        String areaTo = String.valueOf(Utils.getRandomShortInt());
        basePageSteps.onMapPage().extendFilters().byExactName(AREA_LOT).input("до").sendKeys(areaTo + Keys.ENTER);
        basePageSteps.loaderWait();
        urlSteps.queryParam(LOT_AREA_MAX, areaTo).shouldNotDiffWithWebDriverUrl();
        basePageSteps.shouldSeeRequestInBrowser(hasItem(urlParam(LOT_AREA_MAX, areaTo)));
    }
}
