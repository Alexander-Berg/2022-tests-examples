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

@DisplayName("Расширенные фильтры. Дополнительно")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebMobileModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ExtendedFiltersExtraChpuTest {

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
                {"От собственников", "/bez-posrednikov/"}
        });
    }

    @Before
    public void before() {
        basePageSteps.resize(1400, 3000);
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onMobileMainPage().openExtFilter();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Блок «Дополнительно»")
    public void shouldSeeExtraFilters() {
        basePageSteps.scrollToElement(basePageSteps.onMobileMainPage().extendFilters().button(label));
        basePageSteps.onMobileMainPage().extendFilters().button(label).click();
        basePageSteps.onMobileMainPage().extendFilters().applyFiltersButton().click();
        urlSteps.path(KUPIT).path(KVARTIRA).path(expected).shouldNotDiffWithWebDriverUrl();
    }
}
