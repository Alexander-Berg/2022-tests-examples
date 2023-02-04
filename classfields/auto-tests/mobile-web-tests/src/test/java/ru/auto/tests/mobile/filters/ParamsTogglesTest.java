package ru.auto.tests.mobile.filters;

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
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.Owners.DENISKOROBOV;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.ON_CREDIT;
import static ru.auto.tests.desktop.consts.Pages.USED;

@DisplayName("Расширенные фильтры - тумблеры")
@Feature(AutoruFeatures.FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ParamsTogglesTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String section;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {ALL},
                {USED}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(CARS).path(section).open();
        basePageSteps.onListingPage().filters().paramsButton().click();
        basePageSteps.hideApplyFiltersButton();
    }

    @Test
    @DisplayName("Тумблеры")
    @Category({Regression.class})
    @Owner(DENISKOROBOV)
    public void shouldClickToggle() {
        basePageSteps.onListingPage().paramsPopup().inactiveToggle("В кредит").click();
        basePageSteps.showApplyFiltersButton();
        basePageSteps.onMainPage().paramsPopup().applyFiltersButton().click();
        urlSteps.path(ON_CREDIT).shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().paramsButton().click();
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onListingPage().paramsPopup().activeToggle("В кредит").click();
        basePageSteps.showApplyFiltersButton();
        basePageSteps.onListingPage().paramsPopup().applyFiltersButton().click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(section).shouldNotSeeDiff();
    }
}
