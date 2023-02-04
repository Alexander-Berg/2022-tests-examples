package ru.auto.tests.cabinet.calculator.agency.msk;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CalculatorPageSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.String.valueOf;
import static org.apache.commons.lang3.RandomUtils.nextInt;
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
@DisplayName("Кабинет дилера. Калькулятор. Агентский дилер(Москва). Легковые новые")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class CalculatorNewCarsBlockTest {

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
        mockRule.newMock().with("cabinet/Session/AgencyDealerMoscow",
                        "cabinet/ApiAccessClient",
                        "cabinet/DealerTariff/CarsNewCallsOff",
                        "cabinet/DesktopClientsGet/AgencyDealer",
                        "cabinet/CommonCustomerGet").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(CALCULATOR).open();
    }

    @Test
    @Category({Regression.class, Screenshooter.class, Testing.class})
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
    @Ignore
    @Category({Regression.class, Testing.class})
    @Owner(JENKL)
    @DisplayName("Количество звонков за период при отключённом тарифе")
    public void shouldSeeNumberOfCallsPerPeriodByDisabledTariff() {
        steps.expandCalculatorAndBalanceBlocks(steps.onNewCalculatorPage().newCarsBlock());

        int prevAmount = steps.extractSum(steps.onNewCalculatorPage().newCarsBlock().amount());
        Assertions.assertThat(prevAmount).isZero().describedAs("Проверяем что сумма размещения равна нулю");
        steps.onNewCalculatorPage().balance().summary("Легковые новые").should(not(isDisplayed()));

        steps.onNewCalculatorPage().newCarsBlock().numberOfCalls().click();
        steps.onNewCalculatorPage().newCarsBlock().numberOfCalls().clear();
        steps.onNewCalculatorPage().newCarsBlock().numberOfCalls().sendKeys(valueOf(nextInt(1, MAX_VALUE)));

        int amount = steps.extractSum(steps.onNewCalculatorPage().newCarsBlock().amount());
        Assertions.assertThat(amount).isNotZero().describedAs("Проверяем что сумма размещения увеличилась");
        steps.onNewCalculatorPage().balance().summary("Легковые новые").should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(JENKL)
    @DisplayName("Блок «Легковые с пробегом». Услуги при отключённом тарифе")
    public void shouldSeeServices() {
        steps.expandCalculatorAndBalanceBlocks(steps.onNewCalculatorPage().newCarsBlock());
        int prevAmount = steps.extractSum(steps.onNewCalculatorPage().newCarsBlock().amount());
        Assertions.assertThat(prevAmount).isZero().describedAs("Проверяем что сумма размещения равна нулю");
        steps.onNewCalculatorPage().balance().summary("Легковые с").should(not(isDisplayed()));
        steps.onNewCalculatorPage().balance().replenish().should(not(isDisplayed()));

        steps.addServiceInBlock(PREMIUM_SERVICE, steps.onNewCalculatorPage().newCarsBlock());

        int amount = steps.extractSum(steps.onNewCalculatorPage().newCarsBlock().amount());
        Assertions.assertThat(amount).isNotZero().describedAs("Проверяем что сумма размещения увеличилась");
        steps.onNewCalculatorPage().balance().summary("Легковые с").should(not(isDisplayed()));
        steps.onNewCalculatorPage().balance().replenish().should(not(isDisplayed()));
    }
}
