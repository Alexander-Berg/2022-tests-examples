package ru.auto.tests.cabinet.calculator.agency.regions;

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

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.JENKL;
import static ru.auto.tests.desktop.consts.Pages.CALCULATOR;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.element.cabinet.calculator.Services.PREMIUM_SERVICE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 10.12.18
 */
@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Калькулятор. Агентский дилер (Регионы). Коммерческий транспорт")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class CalculatorKomTCBlockWithTariffOnTest {

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
        mockRule.newMock().with("cabinet/Session/AgencyDealerRegions",
                        "cabinet/ApiAccessClient",
                        "cabinet/DealerTariff/TrucksNewUsedOn",
                        "cabinet/DesktopClientsGet/AgencyDealer",
                        "cabinet/CommonCustomerGet").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(CALCULATOR).open();
    }

    @Test
    @Category({Regression.class, Screenshooter.class, Testing.class})
    @Owner(JENKL)
    @DisplayName("Блок «Коммерческий транспорт». Включённый тариф. Скриншот")
    public void shouldSeeKomTCBlock() {
        steps.onNewCalculatorPage().komTCBlock().click();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onNewCalculatorPage().komTCBlock());

        urlSteps.onCurrentUrl().setProduction().open();
        steps.onNewCalculatorPage().komTCBlock().click();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onNewCalculatorPage().komTCBlock());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(JENKL)
    @DisplayName("Блок «Коммерческий транспорт». Услуги при включённом тарифе")
    public void shouldSeeServicesByActiveTariff() {
        steps.onNewCalculatorPage().komTCBlock().click();
        steps.expandOnlyServicesInCategoriesSummaryBlock();

        int prevAmount = steps.extractSum(steps.onNewCalculatorPage().komTCBlock().amount());
        int prevServInCat = steps.extractSum(steps.onNewCalculatorPage().balance()
                .summary(Balance.SERVICES_IN_CATEGORIES_SUMMARY));
        int prevMoto = steps.extractSum(steps.onNewCalculatorPage().balance().summary("Коммерческий транспорт"));
        int prevResult = steps.extractSum(steps.onNewCalculatorPage().balance().summary("Итог"));
        steps.onNewCalculatorPage().balance().replenish().should(not(isDisplayed()));

        steps.addServiceInBlock(PREMIUM_SERVICE, steps.onNewCalculatorPage().komTCBlock());

        steps.shouldSeeAmountIncrease(prevAmount, steps.onNewCalculatorPage().komTCBlock().amount());
        steps.shouldSeeAmountIncrease(prevServInCat, steps.onNewCalculatorPage().balance()
                .summary(Balance.SERVICES_IN_CATEGORIES_SUMMARY));
        steps.shouldSeeAmountIncrease(prevMoto, steps.onNewCalculatorPage().balance().summary("С\u00a0пробегом"));
        steps.shouldSeeAmountIncrease(prevResult, steps.onNewCalculatorPage().balance().summary("Итог"));
        steps.onNewCalculatorPage().balance().replenish().should(not(isDisplayed()));
    }
}
