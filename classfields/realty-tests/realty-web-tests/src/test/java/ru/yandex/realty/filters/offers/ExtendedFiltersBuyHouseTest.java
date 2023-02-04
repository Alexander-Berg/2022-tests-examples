package ru.yandex.realty.filters.offers;

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
import ru.auto.tests.commons.util.Utils;
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.Matchers.hasItem;
import static ru.yandex.realty.consts.Filters.DOM;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Owners.KOPITSA;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.step.BasePageSteps.urlParam;

@DisplayName("Расширенные фильтры поиска по объявлениям.")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ExtendedFiltersBuyHouseTest {

    public static final String LOT_AREA_MIN = "lotAreaMin";
    public static final String LOT_AREA_MAX = "lotAreaMax";
    public static final String AREA_LOT = "Площадь участка";
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps user;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void openSaleAdsPage() {
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(DOM).open();
        user.onOffersSearchPage().openExtFilter();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KOPITSA)
    @DisplayName("Параметр «Участок от»")
    public void shouldSeeLotAreaMinInUrl() {
        String areaFrom = String.valueOf(Utils.getRandomShortInt());
        user.onOffersSearchPage().extendFilters().byExactName(AREA_LOT).input("от").sendKeys(areaFrom);
        user.onOffersSearchPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam(LOT_AREA_MIN, areaFrom).shouldNotDiffWithWebDriverUrl();
        user.shouldSeeRequestInBrowser(hasItem(urlParam(LOT_AREA_MIN, areaFrom)));
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KOPITSA)
    @DisplayName("Параметр «Участок до»")
    public void shouldSeeLotAreaMaxInUrl() {
        String areaTo = String.valueOf(Utils.getRandomShortInt());
        user.onOffersSearchPage().extendFilters().byExactName(AREA_LOT).input("до").sendKeys(areaTo);
        user.onOffersSearchPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam(LOT_AREA_MAX, areaTo).shouldNotDiffWithWebDriverUrl();
        user.shouldSeeRequestInBrowser(hasItem(urlParam(LOT_AREA_MAX, areaTo)));
    }
}
