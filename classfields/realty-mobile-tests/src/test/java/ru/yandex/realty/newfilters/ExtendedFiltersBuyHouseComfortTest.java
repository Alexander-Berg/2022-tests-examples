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
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.realty.consts.Filters.DOM;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.mobile.page.MainPage.DOM_OPTION;
import static ru.yandex.realty.mobile.page.MainPage.KVARTIRU_OPTION;

@DisplayName("Расширенные фильтры поиска по объявлениям. Инфраструктура")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebMobileModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ExtendedFiltersBuyHouseComfortTest {

    private static final String INFRASTRUCTURE = "Инфраструктура";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String label;

    @Parameterized.Parameter(1)
    public String expected;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testParams() {
        return asList(new Object[][]{
                {"Канализация", "hasSewerageSupply"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA_I_MO).open();
        basePageSteps.onMobileMainPage().openExtFilter();
        basePageSteps.selectOption(
                basePageSteps.onMobileMainPage().extendFilters().selector(KVARTIRU_OPTION), DOM_OPTION);
        basePageSteps.scrollToElement(basePageSteps.onMobileMainPage().extendFilters().byName(INFRASTRUCTURE));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Инфраструктура»")
    public void shouldSeeComfortFiltersInUrl() {
        basePageSteps.onMobileMainPage().extendFilters().byName(INFRASTRUCTURE).button(label).click();
        basePageSteps.onMobileMainPage().extendFilters().applyFiltersButton().click();
        urlSteps.path(KUPIT).path(DOM).queryParam(expected, "YES").shouldNotDiffWithWebDriverUrl();
    }
}
