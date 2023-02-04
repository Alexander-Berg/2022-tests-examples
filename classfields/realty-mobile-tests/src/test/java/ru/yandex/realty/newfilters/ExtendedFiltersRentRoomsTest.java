package ru.yandex.realty.newfilters;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
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
import static ru.yandex.realty.consts.Filters.KOMNATA;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Filters.SNYAT;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.mobile.page.MainPage.KOMNATU_OPTION;
import static ru.yandex.realty.mobile.page.MainPage.KUPIT_OPTION;
import static ru.yandex.realty.mobile.page.MainPage.KVARTIRU_OPTION;
import static ru.yandex.realty.mobile.page.MainPage.SNYAT_OPTION;

@DisplayName("Расширенные фильтры. В квартире")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebMobileModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ExtendedFiltersRentRoomsTest {

    private static final String IN_FLAT = "В квартире";

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
//                {"2 комнаты", "/dvuhkomnatnaya/"},
                {"3 комнаты", "/tryohkomnatnaya/"},
                {"4 комнаты и более", "/4-i-bolee/"},
        });
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «В квартире»")
    public void shouldSeeRentRoomsInUrl() {
        urlSteps.testing().path(MOSKVA_I_MO).open();
        basePageSteps.onMobileMainPage().openExtFilter();
        basePageSteps.selectOption(
                basePageSteps.onMobileMainPage().extendFilters().selector(KUPIT_OPTION), SNYAT_OPTION);
        basePageSteps.selectOption(
                basePageSteps.onMobileMainPage().extendFilters().selector(KVARTIRU_OPTION), KOMNATU_OPTION);
        basePageSteps.scrollToElement(basePageSteps.onMobileMainPage().extendFilters().byName(IN_FLAT));
        basePageSteps.onMobileMainPage().extendFilters().byName(IN_FLAT).button(label).click();
        basePageSteps.onMobileMainPage().extendFilters().applyFiltersButton().click();
        urlSteps.path(SNYAT).path(KOMNATA).path(expected).shouldNotDiffWithWebDriverUrl();
    }
}
