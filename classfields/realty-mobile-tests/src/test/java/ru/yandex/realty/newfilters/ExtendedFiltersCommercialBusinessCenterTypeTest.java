package ru.yandex.realty.newfilters;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.yandex.realty.consts.Filters.COMMERCIAL;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.OFIS;
import static ru.yandex.realty.consts.Filters.SPB_I_LO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.step.UrlSteps.CATEGORY_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.COMMERCIAL_TYPE_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.COMMERCIAL_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.OFFICE_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.RGID;
import static ru.yandex.realty.step.UrlSteps.SELL_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.SPB_I_LO_RGID;
import static ru.yandex.realty.step.UrlSteps.TYPE_URL_PARAM;

@DisplayName("Фильтры поиска по коммерческой недвижимости. Класс БЦ")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebMobileModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ExtendedFiltersCommercialBusinessCenterTypeTest {

    private static final String CLASS = "Класс";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;
    @Parameterized.Parameter
    public String title;

    @Parameterized.Parameter(1)
    public String label;

    @Parameterized.Parameter(2)
    public String expected;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {"A плюс", "A+", "a-plus"},
                {"A", "A", "a"},
                {"B плюс", "B+", "b-plus"},
                {"B", "B", "b"},
                {"C плюс", "C+", "c-plus"},
                {"C", "C", "c"}
        });
    }


    @Before
    public void before() {
        urlSteps.testing().path(Pages.FILTERS).queryParam(RGID, SPB_I_LO_RGID)
                .queryParam(COMMERCIAL_TYPE_URL_PARAM, OFFICE_URL_PARAM)
                .queryParam("commercialBuildingType", "BUSINESS_CENTER").queryParam(TYPE_URL_PARAM, SELL_URL_PARAM)
                .queryParam(CATEGORY_URL_PARAM, COMMERCIAL_URL_PARAM).open();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Класс» бизнес центра")
    public void shouldSeeBusinessCenterClassInUrl() {
        basePageSteps.scrollToElement(basePageSteps.onMobileMainPage().searchFilters().byName(CLASS));
        basePageSteps.onMobileMainPage().searchFilters().byName(CLASS).buttonWithText(label).click();
        basePageSteps.onMobileMainPage().searchFilters().applyFiltersButton().click();
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(COMMERCIAL).path(OFIS)
                .path(format("/biznes-center-i-class-%s/", expected)).shouldNotDiffWithWebDriverUrl();
    }
}
