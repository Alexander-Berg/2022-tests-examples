package ru.auto.tests.cabinet.agency.main;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.AgencyCabinetPagesSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.AGENCY_CABINET;
import static ru.auto.tests.desktop.consts.Owners.CHERNOVPA;
import static ru.auto.tests.desktop.consts.Pages.DASHBOARD;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_AGENCY;
import static ru.auto.tests.desktop.mock.MockStub.stub;

@Feature(AGENCY_CABINET)
@DisplayName("Кабинет агента. Главная страница")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class AgencyCabinetMainPageTest {
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

    @Before
    public void before() {
        mockRule.setStubs(
                stub("cabinet/SessionAgency"),
                stub("cabinet/DealerAccountAgency"),
                stub("cabinet/CommonCustomerGetAgency"),
                stub("cabinet/AgencyAgencyGet"),
                stub("cabinet/AgencyBillingRenewalAutorenewalGet"),
                stub("cabinet/AgencyClientsPresetsGet"),
                stub("cabinet/CabinetCustomerStatistics"),
                stub("cabinet/DealerWalletProductActivationsTotalStats")
        ).create();

        urlSteps.subdomain(SUBDOMAIN_AGENCY).path(DASHBOARD).open();
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(CHERNOVPA)
    @DisplayName("Круговая диаграмма")
    public void shouldSeePieChartDiagram() {
        steps.waitUntilPageIsFullyLoaded();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onAgencyCabinetMainPage().pieChart());

        urlSteps.setProduction().open();
        steps.waitUntilPageIsFullyLoaded();

        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onAgencyCabinetMainPage().pieChart());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }
}
