package ru.yandex.realty.newfilters;

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
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.realty.consts.Filters.KOTTEDZHNYE_POSELKI;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.step.UrlSteps.CATEGORY_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.HOUSE_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.MO_RGID;
import static ru.yandex.realty.step.UrlSteps.OBJECT_TYPE_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.VILLAGE_URL_PARAM;

@DisplayName("Фильтр поиска по коттеджным поселкам. Рядом")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class ExtendedFiltersVillageNearZhDTest {

    private static final String NEARBY = "Рядом";

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
    @DisplayName("Параметр «Рядом ж/д станция»")
    public void shouldSeeNearZhd() {
        urlSteps.testing().path(Pages.FILTERS).queryParam("rgid", MO_RGID).villageFiltersMobile().open();
        basePageSteps.scrollToElement(basePageSteps.onMobileMainPage().searchFilters().byName(NEARBY));
        basePageSteps.onMobileMainPage().searchFilters().button("Ж/Д станция").click();
        basePageSteps.onMobileMainPage().searchFilters().applyFiltersButton().click();
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KOTTEDZHNYE_POSELKI)
                .queryParam("hasRailwayStation", "YES").queryParam(CATEGORY_URL_PARAM, HOUSE_URL_PARAM)
                .queryParam(OBJECT_TYPE_URL_PARAM, VILLAGE_URL_PARAM).shouldNotDiffWithWebDriverUrl();
    }
}
