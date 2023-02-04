package ru.yandex.realty.filters.commercial;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.qatools.htmlelements.matchers.WebElementMatchers;
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Smoke;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.realty.consts.Filters.COMMERCIAL;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.OFIS;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;

@DisplayName("Фильтры поиска по коммерческой недвижимости")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ExtendedFiltersBuisenessCenterClassTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps user;

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
    public void openCommercialPage() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(COMMERCIAL).path(OFIS).path("/biznes-center/").open();
        user.onCommercialPage().openExtFilter();
    }

    @Test
    @Category({Regression.class, Smoke.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Класс бизнес центра»")
    public void shouldSeeBusinessCenterClassInUrl() {
        user.onCommercialPage().extendFilters().classButton(label).waitUntil(WebElementMatchers.isDisplayed()).click();
        user.onCommercialPage().extendFilters().classButton(label).waitUntil(hasClass(containsString("_checked")), 1);
        user.onCommercialPage().extendFilters().applyFiltersButton().click();
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(COMMERCIAL).path(OFIS)
                .path(format("/biznes-center-i-class-%s/", expected)).shouldNotDiffWithWebDriverUrl();
    }
}
