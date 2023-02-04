package ru.auto.tests.cabinet.calculator;

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
import static ru.auto.tests.desktop.element.cabinet.calculator.KomTCCalculatorBlock.HEAVE_COMMERCIAL_VEHICLES_USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Калькулятор. Скриншоты")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class CalculatorScreenshotTests {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private CalculatorPageSteps steps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthDealer",
                "cabinet/ApiAccessClient",
                "cabinet/CommonCustomerGet",
                "cabinet/DealerAccount",
                "cabinet/DealerCampaigns",
                "cabinet/DealerTariff",
                "cabinet/ClientsGet").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(CALCULATOR).open();
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(JENKL)
    @DisplayName("Блок «Мото». Скриншот")
    public void shouldSeeMotoBlock() {
        steps.onNewCalculatorPage().motoBlock().click();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onNewCalculatorPage().motoBlock());

        urlSteps.onCurrentUrl().setProduction().open();
        steps.onNewCalculatorPage().motoBlock().click();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onNewCalculatorPage().motoBlock());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(JENKL)
    @DisplayName("Блок «Легковые новые». Скриншот")
    public void shouldSeeNewCarsBlock() {
        steps.onNewCalculatorPage().newCarsBlock().click();
        steps.onNewCalculatorPage().newCarsBlock().services().should(isDisplayed());
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onNewCalculatorPage().newCarsBlock());

        urlSteps.onCurrentUrl().setProduction().open();
        steps.onNewCalculatorPage().newCarsBlock().click();
        steps.onNewCalculatorPage().newCarsBlock().services().should(isDisplayed());
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onNewCalculatorPage().newCarsBlock());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(JENKL)
    @DisplayName("Блок «Легковые с пробегом». Скриншот")
    public void shouldSeeUsedCarsBlock() {
        steps.onNewCalculatorPage().usedCarsBlock().click();
        steps.onNewCalculatorPage().usedCarsBlock().services().should(isDisplayed());
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onNewCalculatorPage().usedCarsBlock());

        urlSteps.onCurrentUrl().setProduction().open();
        steps.onNewCalculatorPage().usedCarsBlock().click();
        steps.onNewCalculatorPage().usedCarsBlock().services().should(isDisplayed());
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onNewCalculatorPage().usedCarsBlock());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(JENKL)
    @DisplayName("Блок «Коммерческий транспорт». Скриншот")
    public void shouldSeeKomTCBlock() {
        steps.onNewCalculatorPage().komTCBlock().click();
        steps.onNewCalculatorPage().komTCBlock().tab(HEAVE_COMMERCIAL_VEHICLES_USED).click();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onNewCalculatorPage().komTCBlock());

        urlSteps.onCurrentUrl().setProduction().open();
        steps.onNewCalculatorPage().komTCBlock().click();
        steps.onNewCalculatorPage().komTCBlock().tab(HEAVE_COMMERCIAL_VEHICLES_USED).click();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onNewCalculatorPage().komTCBlock());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(JENKL)
    @DisplayName("Блок с балансом. Скриншот")
    public void shouldSeeBalanceBlock() {
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onNewCalculatorPage().balance());

        urlSteps.onCurrentUrl().setProduction().open();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onNewCalculatorPage().balance());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(JENKL)
    @DisplayName("Блок с балансом в развернутом состоянии. Скриншот")
    public void shouldSeeBalanceBlockInOpenState() {
        steps.onNewCalculatorPage().balance().summary(Balance.ACCOMMODATION_IN_CATEGORIES_SUMMARY).click();
        steps.onNewCalculatorPage().balance().summary(Balance.SERVICES_IN_CATEGORIES_SUMMARY).click();

        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onNewCalculatorPage().balance());

        urlSteps.onCurrentUrl().setProduction().open();
        steps.onNewCalculatorPage().balance().summary(Balance.ACCOMMODATION_IN_CATEGORIES_SUMMARY).click();
        steps.onNewCalculatorPage().balance().summary(Balance.SERVICES_IN_CATEGORIES_SUMMARY).click();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onNewCalculatorPage().balance());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(JENKL)
    @DisplayName("Калькулятор. Свернутые блоки. Прямой дилер. Общий скриншот")
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
}
