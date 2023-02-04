package ru.auto.tests.cabinet.calculator.direct.msk;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.assertj.core.api.Assertions;
import org.junit.Before;
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
@DisplayName("Кабинет дилера. Калькулятор. Прямой дилер(Москва). Легковые с пробегом")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class CalculatorUsedCarsBlockTest {

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
                "cabinet/DealerTariff/CarsUsedOff",
                "cabinet/DesktopClientsGet/Dealer",
                "cabinet/CommonCustomerGet").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(CALCULATOR).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(JENKL)
    @DisplayName("Кнопка «Подключить тариф»")
    public void shouldSeeTurnOnTariff() {
        steps.expandCalculatorAndBalanceBlocks(steps.onNewCalculatorPage().usedCarsBlock());
        steps.onNewCalculatorPage().balance().summary("Легковые с").should(not(isDisplayed()));

        mockRule.delete();
        mockRule.newMock().with("cabinet/Session/DirectDealerMoscow",
                        "cabinet/ApiAccessClient",
                        "cabinet/DealerTariff/CarsUsedOn",
                        "cabinet/ApiServiceAutoruAdsRequestCarsUsedClientPost").post();

        steps.onNewCalculatorPage().usedCarsBlock().button("Подключить тариф").click();
        steps.onNewCalculatorPage().serviceStatusPopup("Ваш тариф успешно изменен").should(isDisplayed());
        steps.onNewCalculatorPage().balance().summary("Легковые с").should(isDisplayed());
    }

    @Test
    @Category({Regression.class, Screenshooter.class, Testing.class})
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
    @Category({Regression.class, Testing.class})
    @Owner(JENKL)
    @DisplayName("Размещение объявлений при отлючённом тарифе")
    public void shouldSeeChangedAmountInAccommodationInCategoriesByDisabledTariff() {
        steps.expandCalculatorAndBalanceBlocks(steps.onNewCalculatorPage().usedCarsBlock());
        int prevAmount = steps.extractSum(steps.onNewCalculatorPage().usedCarsBlock().amount());
        Assertions.assertThat(prevAmount).isZero().describedAs("Проверяем что сумма размещения равна нулю");
        steps.onNewCalculatorPage().balance().summary("Легковые с").should(not(isDisplayed()));

        steps.onNewCalculatorPage().usedCarsBlock().input("До 300 тыс.").click();
        steps.onNewCalculatorPage().usedCarsBlock().input("До 300 тыс.").sendKeys(valueOf(nextInt(1, MAX_VALUE)));

        int amount = steps.extractSum(steps.onNewCalculatorPage().usedCarsBlock().amount());
        Assertions.assertThat(amount).isNotZero().describedAs("Проверяем что сумма размещения увеличилась");
        steps.onNewCalculatorPage().balance().summary("Легковые с").should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(JENKL)
    @DisplayName("Блок «Легковые с пробегом». Услуги при отключённом тарифе")
    public void shouldSeeServices() {
        steps.expandCalculatorAndBalanceBlocks(steps.onNewCalculatorPage().usedCarsBlock());
        int prevAmount = steps.extractSum(steps.onNewCalculatorPage().usedCarsBlock().amount());
        Assertions.assertThat(prevAmount).isZero().describedAs("Проверяем что сумма размещения равна нулю");
        steps.onNewCalculatorPage().balance().summary("Легковые с").should(not(isDisplayed()));

        steps.addServiceInBlock(PREMIUM_SERVICE, steps.onNewCalculatorPage().usedCarsBlock());

        int amount = steps.extractSum(steps.onNewCalculatorPage().usedCarsBlock().amount());
        Assertions.assertThat(amount).isNotZero().describedAs("Проверяем что сумма размещения увеличилась");
        steps.onNewCalculatorPage().balance().summary("Легковые с").should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(JENKL)
    @DisplayName("Информация об услуге")
    public void shouldSeeTooltip() {
        steps.onNewCalculatorPage().usedCarsBlock().click();

        steps.moveCursor(steps.onNewCalculatorPage().usedCarsBlock().services().service(PREMIUM_SERVICE).tooltip());
        steps.onNewCalculatorPage().activePopup().should(isDisplayed());
    }
}
