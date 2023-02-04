package ru.auto.tests.cabinet.calculator.direct.msk;

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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.element.cabinet.calculator.Balance;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CalculatorPageSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.JENKL;
import static ru.auto.tests.desktop.consts.Pages.CALCULATOR;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 10.12.18
 */
@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Калькулятор. Прямой дилер")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class CalculatorPageTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private CalculatorPageSteps steps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Before
    public void before() {
        mockRule.newMock().with("cabinet/Session/DirectDealerMoscow",
                        "cabinet/ApiAccessClient",
                        "cabinet/DesktopClientsGet/Dealer",
                        "cabinet/CommonCustomerGet",
                        "cabinet/DealerTariff/AllTariffs").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(CALCULATOR).open();
    }

    @Test
    @Category({Regression.class, Screenshooter.class, Testing.class})
    @Owner(JENKL)
    @DisplayName("Калькулятор. Скриншот")
    public void shouldSeeCalculator() {
        steps.onNewCalculatorPage().linkOnTariffs().hover();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onNewCalculatorPage().calculator());

        urlSteps.onCurrentUrl().setProduction().open();
        steps.onNewCalculatorPage().linkOnTariffs().hover();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onNewCalculatorPage().calculator());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Screenshooter.class, Testing.class})
    @Owner(JENKL)
    @DisplayName("Блок с балансом. Скриншот")
    public void shouldSeeBalanceBlock() {
        Screenshot testingScreenshot = screenshotSteps.getElementScreenshotWithWaiting(steps.onNewCalculatorPage().balance());

        urlSteps.onCurrentUrl().setProduction().open();
        Screenshot productionScreenshot = screenshotSteps.getElementScreenshotWithWaiting(steps.onNewCalculatorPage().balance());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Screenshooter.class, Testing.class})
    @Owner(JENKL)
    @DisplayName("Блок с балансом в развернутом состоянии. Скриншот")
    public void shouldSeeBalanceBlockInOpenState() {
        steps.onNewCalculatorPage().balance().summary(Balance.ACCOMMODATION_IN_CATEGORIES_SUMMARY).click();
        steps.onNewCalculatorPage().balance().summary(Balance.SERVICES_IN_CATEGORIES_SUMMARY).click();

        Screenshot testingScreenshot = screenshotSteps.getElementScreenshotWithWaiting(steps.onNewCalculatorPage().balance());

        urlSteps.onCurrentUrl().setProduction().open();
        steps.onNewCalculatorPage().balance().summary(Balance.ACCOMMODATION_IN_CATEGORIES_SUMMARY).click();
        steps.onNewCalculatorPage().balance().summary(Balance.SERVICES_IN_CATEGORIES_SUMMARY).click();
        Screenshot productionScreenshot = screenshotSteps.getElementScreenshotWithWaiting(steps.onNewCalculatorPage().balance());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(JENKL)
    @DisplayName("Ссылка «Тарифы на размещение объявлений и дополнительные услуги»")
    public void shouldSeeDealerCostPage() {
        steps.onNewCalculatorPage().linkOnTariffs().click();
        urlSteps.shouldNotDiffWith("https://auto.ru/dealer/#cost");
    }
}
