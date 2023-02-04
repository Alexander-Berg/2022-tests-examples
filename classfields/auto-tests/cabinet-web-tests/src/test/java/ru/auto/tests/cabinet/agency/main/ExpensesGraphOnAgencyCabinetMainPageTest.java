package ru.auto.tests.cabinet.agency.main;

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
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.AgencyCabinetPagesSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.AGENCY_CABINET;
import static ru.auto.tests.desktop.consts.Owners.JENKL;
import static ru.auto.tests.desktop.consts.Pages.DASHBOARD;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_AGENCY;
import static ru.auto.tests.desktop.mock.MockStub.stub;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 14.09.18
 */
@Feature(AGENCY_CABINET)
@DisplayName("Кабинет агента. Главная страница")
@GuiceModules(CabinetTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ExpensesGraphOnAgencyCabinetMainPageTest {
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private AgencyCabinetPagesSteps steps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    //@Parameter("Расходы")
    @Parameterized.Parameter
    public String expenses;

    @Parameterized.Parameters(name = "{index}: Расходы «{0}»")
    public static Collection<String> getParameters() {
        return asList(
                "Все расходы",
                "Поднятие в поиске"
        );
    }

    @Before
    public void before() {
        mockRule.setStubs(
                stub("cabinet/SessionAgency"),
                stub("cabinet/DealerAccountAgency"),
                stub("cabinet/CommonCustomerGetAgency"),
                stub("cabinet/AgencyClientsPresetsGet"),
                stub("cabinet/CabinetCustomerStatistics")
        ).create();

        urlSteps.subdomain(SUBDOMAIN_AGENCY).path(DASHBOARD).open();
        steps.waitUntilPageIsFullyLoaded();
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(JENKL)
    @DisplayName("График расходов")
    public void shouldSeeExpensesGraph() {
        steps.onAgencyCabinetMainPage().expensesGraph().selectItem("Все расходы", expenses);
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onAgencyCabinetMainPage().expensesGraph());

        urlSteps.setProduction().open();
        steps.waitUntilPageIsFullyLoaded();
        steps.onAgencyCabinetMainPage().expensesGraph().selectItem("Все расходы", expenses);
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onAgencyCabinetMainPage().expensesGraph());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }
}
