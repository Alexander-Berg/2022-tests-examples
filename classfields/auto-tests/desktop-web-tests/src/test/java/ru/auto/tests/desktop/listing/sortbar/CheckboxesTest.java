package ru.auto.tests.desktop.listing.sortbar;

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

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FILTERS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.LCV;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;

@DisplayName("Панель сортировок - чекбоксы")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CheckboxesTest {

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
    public String checkboxTitle;

    @Parameterized.Parameter(3)
    public String param;

    @Parameterized.Parameter(4)
    public String paramValue;

    @Parameterized.Parameters(name = "name = {index}: {0} {1} {2} {3} {4}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, ALL, "Продажа с НДС", "only_nds", "true"},
                {MOTORCYCLE, ALL, "Продажа с НДС", "only_nds", "true"},
                {LCV, ALL, "Продажа с НДС", "only_nds", "true"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(category).path(section).open();
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по чекбоксу")
    public void shouldClickCheckbox() {
        basePageSteps.onListingPage().sortBar().checkbox(checkboxTitle).click();

        urlSteps.addParam(param, paramValue).shouldNotSeeDiff();
    }
}