package ru.auto.tests.cabinet.calculator.agency;

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
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CalculatorPageSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.JENKL;
import static ru.auto.tests.desktop.consts.Pages.CALCULATOR;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Калькулятор. Скриншоты")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class CalculatorScreenshotTests {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps steps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Before
    public void before() {
        mockRule.newMock().with("cabinet/Session/AgencyDealerMoscow",
                "cabinet/ApiAccessClient",
                "cabinet/DealerTariff/TrucksOff",
                "cabinet/CommonCustomerGet").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(CALCULATOR).open();
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(JENKL)
    @DisplayName("Калькулятор. Свернутые блоки. Агентский дилер. Общий скриншот")
    public void shouldSeeAgencyCalculator() {
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onNewCalculatorPage().calculator().hover());

        urlSteps.onCurrentUrl().setProduction().open();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onNewCalculatorPage().calculator().hover());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }
}
