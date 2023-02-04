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
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.AVTOSERVIS;
import static ru.yandex.realty.consts.Filters.COMMERCIAL;
import static ru.yandex.realty.consts.Filters.GOSTINICA;
import static ru.yandex.realty.consts.Filters.GOTOVYJ_BIZNES;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.OBSHCHEPIT;
import static ru.yandex.realty.consts.Filters.OFIS;
import static ru.yandex.realty.consts.Filters.POMESHCHENIE_SVOBODNOGO_NAZNACHENIYA;
import static ru.yandex.realty.consts.Filters.PROIZVODSTVENNOE_POMESHCHENIE;
import static ru.yandex.realty.consts.Filters.SKLADSKOE_POMESHCHENIE;
import static ru.yandex.realty.consts.Filters.SPB_I_LO;
import static ru.yandex.realty.consts.Filters.TORGOVOE_POMESHCHENIE;
import static ru.yandex.realty.consts.Filters.UCHASTOK_KOMMERCHESKOGO_NAZNACHENIYA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.mobile.element.main.FiltersBlock.FIELD_COMMERCIAL_TYPE;
import static ru.yandex.realty.mobile.page.MainPage.KOMMERCHESKUYU_OPTION;
import static ru.yandex.realty.mobile.page.MainPage.KVARTIRU_OPTION;
import static ru.yandex.realty.rules.MockRuleConfigurable.PATH_TO_OFFER_WITH_SITE_SEARCH_COUNT_TEMPLATE;

@DisplayName("Базовые фильтры. Купить снять")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebMobileModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class BaseCommercialTypeFiltersTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private BasePageSteps user;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String option;

    @Parameterized.Parameter(1)
    public String expected;

    @Parameterized.Parameters(name = "Ищем по типу «{0}»")
    public static Collection<Object[]> rentType() {
        return asList(new Object[][]{
                {"Офисное помещение", OFIS},
                {"Торговое помещение", TORGOVOE_POMESHCHENIE},
                {"Помещение свободного назначения", POMESHCHENIE_SVOBODNOGO_NAZNACHENIYA},
                {"Складское помещение", SKLADSKOE_POMESHCHENIE},
                {"Производственное помещение", PROIZVODSTVENNOE_POMESHCHENIE},
                {"Земельный участок", UCHASTOK_KOMMERCHESKOGO_NAZNACHENIYA},
                {"Общепит", OBSHCHEPIT},
                {"Автосервис", AVTOSERVIS},
                {"Гостиница", GOSTINICA},
                {"Готовый бизнес", GOTOVYJ_BIZNES},
        });
    }

    @Before
    public void openSaleAdsPage() {
        mockRuleConfigurable.offerWithSiteSearchCountStub(
                getResourceAsString(PATH_TO_OFFER_WITH_SITE_SEARCH_COUNT_TEMPLATE))
                .createWithDefaults();
        urlSteps.testing().path(SPB_I_LO).open();
        user.onMobileMainPage().searchFilters().waitUntil(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    public void shouldSeeRentType() {
        user.selectOption(user.onMobileMainPage().searchFilters().selector(KVARTIRU_OPTION), KOMMERCHESKUYU_OPTION);
        user.selectOption(user.onMobileMainPage().searchFilters().selector(KVARTIRU_OPTION), KOMMERCHESKUYU_OPTION);
        user.onMobileMainPage().searchFilters().byName(FIELD_COMMERCIAL_TYPE).click();
        user.onMobileMainPage().searchFilters().filterPopup().tumbler(option).click();
        user.onMobileMainPage().searchFilters().filterPopup().showButton().click();
        user.onMobileMainPage().searchFilters().applyFiltersButton().click();

        urlSteps.path(KUPIT).path(COMMERCIAL).path(expected).shouldNotDiffWithWebDriverUrl();
    }

}
