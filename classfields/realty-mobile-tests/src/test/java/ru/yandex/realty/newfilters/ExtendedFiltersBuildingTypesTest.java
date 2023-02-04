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
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;

@DisplayName("Расширенные фильтры поиска по объявлениям. Тип дома")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebMobileModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ExtendedFiltersBuildingTypesTest {

    private static final String BUILDING_TYPE = "Тип дома";

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
    public String type;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testParams() {
        return asList(new Object[][]{
                {"Кирпич", "/kirpich/"},
                {"Монолит", "/monolit/"},
                {"Панель", "/panel/"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onMobileMainPage().openExtFilter();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Тип здания»")
    public void shouldSeeTypesBuildingInUrl() {
        basePageSteps.scrollToElement(basePageSteps.onMobileMainPage().extendFilters().byName(BUILDING_TYPE));
        basePageSteps.onMobileMainPage().extendFilters().byName(BUILDING_TYPE).button(label).click();
        basePageSteps.onMobileMainPage().extendFilters().applyFiltersButton().click();
        urlSteps.path(KUPIT).path(KVARTIRA).path(type).shouldNotDiffWithWebDriverUrl();
    }
}
