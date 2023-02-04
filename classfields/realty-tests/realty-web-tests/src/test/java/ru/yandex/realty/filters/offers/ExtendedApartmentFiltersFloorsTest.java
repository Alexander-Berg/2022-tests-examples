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

import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;

@DisplayName("Расширенные фильтры поиска по объявлениям.")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ExtendedApartmentFiltersFloorsTest {

    public static final String FLOOR = "Этаж";
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps user;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void openSaleAdsPage() {
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(KVARTIRA).open();
        user.onOffersSearchPage().openExtFilter();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Этаж с»")
    public void shouldSeeFloorMinInUrl() {
        String floorFrom = String.valueOf(Utils.getRandomShortInt());
        user.onOffersSearchPage().extendFilters().byName(FLOOR).input("c").sendKeys(floorFrom);
        user.onOffersSearchPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam("floorMin", floorFrom).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Этаж по»")
    public void shouldSeeFloorMaxInUrl() {
        String floorTo = String.valueOf(Utils.getRandomShortInt());
        user.onOffersSearchPage().extendFilters().byName(FLOOR).input("по").sendKeys(floorTo);
        user.onOffersSearchPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam("floorMax", floorTo).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Только последний этаж»")
    public void shouldSeeLastFloorInUrl() {
        user.onOffersSearchPage().extendFilters().checkButton("Последний");
        user.onOffersSearchPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam("lastFloor", "YES").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Только не последний этаж»")
    public void shouldSeeNotLastFloorInUrl() {
        user.onOffersSearchPage().extendFilters().checkButton("Не последний");
        user.onOffersSearchPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam("lastFloor", "NO").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Не первый этаж»")
    public void shouldSeeNotFirstFloorInUrl() {
        user.onOffersSearchPage().extendFilters().checkButton("Не первый");
        user.onOffersSearchPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam("floorExceptFirst", "YES").shouldNotDiffWithWebDriverUrl();
    }
}
