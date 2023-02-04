package ru.auto.tests.cabinet.calculator.direct.msk;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
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
import static ru.auto.tests.desktop.element.cabinet.calculator.Balance.ACCOMMODATION_IN_CATEGORIES_SUMMARY;
import static ru.auto.tests.desktop.element.cabinet.calculator.Balance.SERVICES_IN_CATEGORIES_SUMMARY;
import static ru.auto.tests.desktop.element.cabinet.calculator.Services.PREMIUM_SERVICE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 10.12.18
 */
@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Калькулятор. Прямой дилер(Москва). Легковые новые")
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
        mockRule.newMock().with("cabinet/Session/DirectDealerMoscow",
                "cabinet/ApiAccessClient",
                "cabinet/DealerTariff/CarsNewCallsOn",
                "cabinet/DesktopClientsGet/Dealer",
                "cabinet/CommonCustomerGet").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(CALCULATOR).open();
    }

    @Test
    @Ignore
    @Category({Regression.class, Testing.class})
    @Owner(JENKL)
    @DisplayName("Количество звонков за период при включённом тарифе")
    public void shouldSeeNumberOfCallsPerPeriod() {
        steps.onNewCalculatorPage().newCarsBlock().click();
        steps.expandOnlyAccommodationInCategoriesSummaryBlock();

        int previousBlockSum = steps.extractSum(steps.onNewCalculatorPage().newCarsBlock().amount());
        int prevSummarySum = steps.extractSum(steps.onNewCalculatorPage().balance().summary("Легковые новые"));
        int prevInCategorySum = steps.extractSum(steps.onNewCalculatorPage().balance()
                .summary(ACCOMMODATION_IN_CATEGORIES_SUMMARY));
        int prevResultSum = steps.extractSum(steps.onNewCalculatorPage().balance()
                .summary("Итог"));

        steps.onNewCalculatorPage().newCarsBlock().numberOfCalls().click();
        steps.onNewCalculatorPage().newCarsBlock().numberOfCalls().clear();
        steps.onNewCalculatorPage().newCarsBlock().numberOfCalls().sendKeys(valueOf(nextInt(1, MAX_VALUE)));

        steps.shouldSeeAmountIncrease(previousBlockSum, steps.onNewCalculatorPage().newCarsBlock().amount());
        steps.shouldSeeAmountIncrease(prevSummarySum, steps.onNewCalculatorPage().balance().summary("Легковые новые"));
        steps.shouldSeeAmountIncrease(prevInCategorySum, steps.onNewCalculatorPage().balance()
                .summary(ACCOMMODATION_IN_CATEGORIES_SUMMARY));
        steps.shouldSeeAmountIncrease(prevResultSum, steps.onNewCalculatorPage().balance()
                .summary("Итог"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(JENKL)
    @DisplayName("Услуги в категориях при включённом тарифе")
    public void shouldSeeChangedAmountInServicesInCategoriesByDisabledTariff() {

        steps.onNewCalculatorPage().newCarsBlock().click();
        steps.expandOnlyServicesInCategoriesSummaryBlock();

        int previousBlockSum = steps.extractSum(steps.onNewCalculatorPage().newCarsBlock().amount());
        int prevSummarySum = steps.extractSum(steps.onNewCalculatorPage().balance().summary("Легковые новые"));
        int prevInCategorySum = steps.extractSum(steps.onNewCalculatorPage().balance()
                .summary(SERVICES_IN_CATEGORIES_SUMMARY));
        int prevResultSum = steps.extractSum(steps.onNewCalculatorPage().balance()
                .summary("Итог"));

        steps.addServiceInBlock(PREMIUM_SERVICE, steps.onNewCalculatorPage().newCarsBlock());

        steps.shouldSeeAmountIncrease(previousBlockSum, steps.onNewCalculatorPage().newCarsBlock().amount());
        steps.shouldSeeAmountIncrease(prevSummarySum, steps.onNewCalculatorPage().balance().summary("Легковые новые"));
        steps.shouldSeeAmountIncrease(prevInCategorySum, steps.onNewCalculatorPage().balance()
                .summary(SERVICES_IN_CATEGORIES_SUMMARY));
        steps.shouldSeeAmountIncrease(prevResultSum, steps.onNewCalculatorPage().balance().summary("Итог"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(JENKL)
    @DisplayName("Кнопка «Отключить тариф»")
    public void shouldSeeTurnOff() {
        steps.expandCalculatorAndBalanceBlocks(steps.onNewCalculatorPage().newCarsBlock());
        steps.onNewCalculatorPage().balance().summary("Легковые новые").should(isDisplayed());

        mockRule.delete();
        mockRule.newMock().with("cabinet/Session/DirectDealerMoscow",
                "cabinet/ApiAccessClient",
                "cabinet/DealerTariff/CarsNewCallsOff",
                "cabinet/ApiServiceAutoruBillingCampaignCallClientPut").post();

        steps.onNewCalculatorPage().newCarsBlock().button("Отключить тариф").click();
        steps.onNewCalculatorPage().serviceStatusPopup("Ваш тариф успешно изменен").should(isDisplayed());
        steps.onNewCalculatorPage().balance().summary("Легковые новые").should(not(isDisplayed()));
    }
}
