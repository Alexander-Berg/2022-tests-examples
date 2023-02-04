package ru.yandex.realty.newfilters;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.realty.consts.Filters.COMMERCIAL;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.step.UrlSteps.CATEGORY_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.COMMERCIAL_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.MOSCOW_RGID;
import static ru.yandex.realty.step.UrlSteps.RGID;
import static ru.yandex.realty.step.UrlSteps.SELL_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.TYPE_URL_PARAM;

@DisplayName("Фильтры поиска по коммерческой недвижимости. Этаж")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class ExtendedFiltersCommercialFloorTest {

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
        urlSteps.testing().path(Pages.FILTERS).queryParam(RGID, MOSCOW_RGID).queryParam(TYPE_URL_PARAM, SELL_URL_PARAM)
                .queryParam(CATEGORY_URL_PARAM, COMMERCIAL_URL_PARAM).open();
        basePageSteps.scrollToElement(basePageSteps.onMobileMainPage().searchFilters().byName("Этаж"));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Этаж»")
    public void shouldSeeFirstFloor() {
        basePageSteps.onMobileMainPage().searchFilters().button("Первый").click();
        basePageSteps.onMobileMainPage().searchFilters().applyFiltersButton().click();
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(COMMERCIAL).queryParam(FLOOR_MIN, "1")
                .queryParam(FLOOR_MAX, "1").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Этаж»")
    public void shouldSeeNotFirstFloor() {
        basePageSteps.onMobileMainPage().searchFilters().button("Выше первого").click();
        basePageSteps.onMobileMainPage().searchFilters().applyFiltersButton().click();
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(COMMERCIAL).queryParam(FLOOR_MIN, "2")
                .shouldNotDiffWithWebDriverUrl();
    }

}
