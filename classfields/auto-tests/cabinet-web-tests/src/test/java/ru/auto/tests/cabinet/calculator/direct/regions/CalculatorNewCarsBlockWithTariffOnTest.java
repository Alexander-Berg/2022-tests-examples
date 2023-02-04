package ru.auto.tests.cabinet.calculator.direct.regions;

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

import static org.assertj.core.api.Assertions.assertThat;
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
@DisplayName("Кабинет дилера. Калькулятор. Прямой дилер (Регионы). Легковые новые")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class CalculatorNewCarsBlockWithTariffOnTest {

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
        mockRule.newMock().with("cabinet/Session/DirectDealerRegions",
                        "cabinet/ApiAccessClient",
                        "cabinet/DealerTariff/CarsNewOn",
                        "cabinet/DesktopClientsGet/Dealer",
                        "cabinet/CommonCustomerGet").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(CALCULATOR).open();
    }

    @Test
    @Category({Regression.class, Screenshooter.class, Testing.class})
    @Owner(JENKL)
    @DisplayName("Блок «Легковые новые». Включённый тариф. Скриншот")
    public void shouldSeeActiveTariffBlock() {
        steps.onNewCalculatorPage().newCarsBlock().click();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onNewCalculatorPage().newCarsBlock());

        urlSteps.onCurrentUrl().setProduction().open();
        steps.onNewCalculatorPage().newCarsBlock().click();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onNewCalculatorPage().newCarsBlock());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(JENKL)
    @DisplayName("Блок «Легковые новые». Выключение тарифа")
    public void shouldSeeTurnOffTariff() {
        steps.expandCalculatorAndBalanceBlocks(steps.onNewCalculatorPage().newCarsBlock());
        assertThat(steps.extractSum(steps.onNewCalculatorPage().newCarsBlock().amount()))
                .isNotZero().describedAs("Проверяем что сумма размещения не равна нулю");
        steps.onNewCalculatorPage().balance().summary("Легковые новые").should(isDisplayed());

        mockRule.delete();
        mockRule.newMock().with("cabinet/Session/DirectDealerRegions",
                "cabinet/ApiAccessClient",
                "cabinet/DealerTariff/CarsNewOff",
                "cabinet/ApiServiceAutoruQuota").post();

        steps.moveCursor(steps.onNewCalculatorPage().newCarsBlock().activeTariff());
        steps.onNewCalculatorPage().newCarsBlock().activeTariff().turnOffTariff().click();
        steps.onNewCalculatorPage().serviceStatusPopup("Ваш тариф успешно изменен").should(isDisplayed());
        steps.onNewCalculatorPage().balance().summary("Легковые новые").should(not(isDisplayed()));
        assertThat(steps.extractSum(steps.onNewCalculatorPage().newCarsBlock().amount()))
                .isZero().describedAs("Проверяем что сумма размещения равна нулю");
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(JENKL)
    @DisplayName("Блок «Легковые новые». Услуги при включённом тарифе")
    public void shouldSeeServicesByActiveTariff() {
        steps.onNewCalculatorPage().newCarsBlock().click();
        steps.expandOnlyServicesInCategoriesSummaryBlock();

        int prevAmount = steps.extractSum(steps.onNewCalculatorPage().newCarsBlock().amount());
        int prevServInCat = steps.extractSum(steps.onNewCalculatorPage().balance()
                .summary(Balance.SERVICES_IN_CATEGORIES_SUMMARY));
        int prevMoto = steps.extractSum(steps.onNewCalculatorPage().balance().summary("Легковые новые"));
        int prevResult = steps.extractSum(steps.onNewCalculatorPage().balance().summary("Итог"));

        steps.addServiceInBlock(PREMIUM_SERVICE, steps.onNewCalculatorPage().newCarsBlock());

        steps.shouldSeeAmountIncrease(prevAmount, steps.onNewCalculatorPage().newCarsBlock().amount());
        steps.shouldSeeAmountIncrease(prevServInCat, steps.onNewCalculatorPage().balance()
                .summary(Balance.SERVICES_IN_CATEGORIES_SUMMARY));
        steps.shouldSeeAmountIncrease(prevMoto, steps.onNewCalculatorPage().balance().summary("Легковые новые"));
        steps.shouldSeeAmountIncrease(prevResult, steps.onNewCalculatorPage().balance().summary("Итог"));
    }
}
