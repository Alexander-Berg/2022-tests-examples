package ru.auto.tests.desktop.listing.filters;

import com.carlosbecker.guice.GuiceModules;
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
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FILTERS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CRANE;
import static ru.auto.tests.desktop.consts.Pages.LCV;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.TRAILER;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Листинг - радио-контролы")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class BaseFiltersRadioTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String section;

    @Parameterized.Parameter(2)
    public String radioButton;

    @Parameterized.Parameter(3)
    public String param;

    @Parameterized.Parameter(4)
    public String paramValue;

    @Parameterized.Parameters(name = "name = {index}: {0} {1} {4}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {LCV, ALL, "до 1 т.", "loading", "%1$s_to=1000"},
                {LCV, ALL, "1-1,5 т.", "loading", "%1$s_from=1000&%1$s_to=1500"},
                {LCV, ALL, "от 1,5 т.", "loading", "%1$s_from=1500"},

                {TRUCK, ALL, "до 3,5 т.", "loading", "%1$s_to=3500"},
                {TRUCK, ALL, "3,5-12 т.", "loading", "%1$s_from=3500&%1$s_to=12000"},
                {TRUCK, ALL, "от 12 т.", "loading", "%1$s_from=12000"},

                {TRAILER, ALL, "до 3,5 т.", "loading", "%1$s_to=3500"},
                {TRAILER, ALL, "3,5-12 т.", "loading", "%1$s_from=3500&%1$s_to=12000"},
                {TRAILER, ALL, "от 12 т.", "loading", "%1$s_from=12000"},

                {CRANE, ALL, "до 3,5 т.", "loading", "%1$s_to=3500"},
                {CRANE, ALL, "3,5-12 т.", "loading", "%1$s_from=3500&%1$s_to=12000"},
                {CRANE, ALL, "от 12 т.", "loading", "%1$s_from=12000"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(category).path(section).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по радио-кнопке")
    public void shouldClickRadioButton() {
        basePageSteps.onListingPage().filter().radioButton(radioButton).click();
        urlSteps.replaceQuery(format(paramValue, param)).shouldNotSeeDiff();
        basePageSteps.onListingPage().filter().resultsButton().waitUntil(isDisplayed()).click();
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onListingPage().salesList().waitUntil(hasSize(greaterThan(0)));
        basePageSteps.onListingPage().filter().radioButtonSelected(radioButton).waitUntil(isDisplayed());
    }
}