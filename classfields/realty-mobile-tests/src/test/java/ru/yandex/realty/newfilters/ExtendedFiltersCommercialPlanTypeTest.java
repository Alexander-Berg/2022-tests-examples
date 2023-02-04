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
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.yandex.realty.consts.Filters.COMMERCIAL;
import static ru.yandex.realty.consts.Filters.OFIS;
import static ru.yandex.realty.consts.Filters.SNYAT;
import static ru.yandex.realty.consts.Filters.SPB_I_LO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.rules.MockRuleConfigurable.PATH_TO_OFFER_WITH_SITE_SEARCH_COUNT_TEMPLATE;
import static ru.yandex.realty.step.UrlSteps.CATEGORY_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.COMMERCIAL_TYPE_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.COMMERCIAL_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.OFFICE_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.RENT_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.RGID;
import static ru.yandex.realty.step.UrlSteps.SPB_I_LO_RGID;
import static ru.yandex.realty.step.UrlSteps.TYPE_URL_PARAM;

@DisplayName("Фильтры поиска по коммерческой недвижимости. Планировка")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebMobileModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ExtendedFiltersCommercialPlanTypeTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String label;

    @Parameterized.Parameter(1)
    public String expected;

    @Parameterized.Parameters(name = "{index} - {0}")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {"Кабинетная", "CABINET"},
                {"Коридорная", "CORRIDOR"},
        });
    }

    @Before
    public void openCommercialPage() {
        mockRuleConfigurable.offerWithSiteSearchCountStub(
                getResourceAsString(PATH_TO_OFFER_WITH_SITE_SEARCH_COUNT_TEMPLATE))
                .createWithDefaults();
        urlSteps.testing().path(Pages.FILTERS).queryParam(RGID, SPB_I_LO_RGID)
                .queryParam(COMMERCIAL_TYPE_URL_PARAM, OFFICE_URL_PARAM).queryParam(TYPE_URL_PARAM, RENT_URL_PARAM)
                .queryParam(CATEGORY_URL_PARAM, COMMERCIAL_URL_PARAM).open();
        basePageSteps.scrollToElement(basePageSteps.onMobileMainPage().searchFilters().byName("Планировка"));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр Планировка")
    public void shouldSeePlanTypeInUrl() {
        basePageSteps.onMobileMainPage().searchFilters().button(label).click();
        basePageSteps.onMobileMainPage().searchFilters().applyFiltersButton().click();
        urlSteps.testing().path(SPB_I_LO).path(SNYAT).path(COMMERCIAL).path(OFIS)
                .queryParam("commercialPlanType", expected).shouldNotDiffWithWebDriverUrl();
    }
}
