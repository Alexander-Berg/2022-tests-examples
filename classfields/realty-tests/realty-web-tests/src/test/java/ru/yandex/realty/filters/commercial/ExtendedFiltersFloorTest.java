package ru.yandex.realty.filters.commercial;

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
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Smoke;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.realty.consts.Filters.COMMERCIAL;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;

@DisplayName("Фильтры поиска по коммерческой недвижимости")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ExtendedFiltersFloorTest {

    private static final String FLOOR_MIN = "floorMin";
    private static final String FLOOR_MAX = "floorMax";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void openCommercialPage() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(COMMERCIAL).open();
        basePageSteps.onCommercialPage().openExtFilter();
        basePageSteps.scrollToElement(basePageSteps.onCommercialPage().extendFilters().byName("Этаж"));
    }

    @Test
    @Category({Regression.class, Smoke.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Этаж»")
    public void shouldSeeFirstFloor() {
        basePageSteps.onCommercialPage().extendFilters().checkButton("Первый");
        basePageSteps.onCommercialPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam(FLOOR_MIN, "1").queryParam(FLOOR_MAX, "1").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Smoke.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Этаж»")
    public void shouldSeeNotFirstFloor() {
        basePageSteps.onCommercialPage().extendFilters().checkButton("Выше первого");
        basePageSteps.onCommercialPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam(FLOOR_MIN, "2").shouldNotDiffWithWebDriverUrl();
    }

}
