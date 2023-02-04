package ru.auto.tests.cabinet.calculator;

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
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CalculatorPageSteps;

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

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Калькулятор. Скриншоты")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)

public class CalculatorPricesTest {

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

    @Ignore //TODO поправить моки, добавить ответ ручки с отключённым тарифоим
    @Test
    @Category({Regression.class})
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

    @Ignore //поправить моки, добавить ответ ручки с отключённым тарифоим
    @Test
    @Category({Regression.class})
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
    @Category({Regression.class})
    @Owner(JENKL)
    @DisplayName("Размещение объявлений при включённом тарифе")
    public void shouldSeeChangedAmountInAccommodationInCategories() {
        steps.expandCalculatorAndBalanceBlocks(steps.onNewCalculatorPage().usedCarsBlock());
        int previousBlockSum = steps.extractSum(steps.onNewCalculatorPage().usedCarsBlock().amount());
        int prevSummarySum = steps.extractSum(steps.onNewCalculatorPage().balance().summary("Легковые с"));
        int prevInCategorySum = steps.extractSum(steps.onNewCalculatorPage().balance()
                .summary(ACCOMMODATION_IN_CATEGORIES_SUMMARY));
        int prevResultSum = steps.extractSum(steps.onNewCalculatorPage().balance()
                .summary("Итог"));

        steps.onNewCalculatorPage().usedCarsBlock().input("До 300 тыс.").click();
        steps.onNewCalculatorPage().usedCarsBlock().input("До 300 тыс.").sendKeys(valueOf(nextInt(1, MAX_VALUE)));

        steps.shouldSeeAmountIncrease(previousBlockSum, steps.onNewCalculatorPage().usedCarsBlock().amount());
        steps.shouldSeeAmountIncrease(prevSummarySum, steps.onNewCalculatorPage().balance().summary("Легковые с"));
        steps.shouldSeeAmountIncrease(prevInCategorySum, steps.onNewCalculatorPage().balance()
                .summary(ACCOMMODATION_IN_CATEGORIES_SUMMARY));
        steps.shouldSeeAmountIncrease(prevResultSum, steps.onNewCalculatorPage().balance()
                .summary("Итог"));
    }

    @Test
    @Category({Regression.class})
    @Owner(JENKL)
    @DisplayName("Услуги в категориях при включённом тарифе")
    public void shouldSeeChangedAmountInServicesInCategoriesByDisabledTariff() {
        steps.onNewCalculatorPage().usedCarsBlock().click();
        steps.expandOnlyServicesInCategoriesSummaryBlock();

        int previousBlockSum = steps.extractSum(steps.onNewCalculatorPage().usedCarsBlock().amount());
        int prevSummarySum = steps.extractSum(steps.onNewCalculatorPage().balance().summary("Легковые с"));
        int prevInCategorySum = steps.extractSum(steps.onNewCalculatorPage().balance()
                .summary(SERVICES_IN_CATEGORIES_SUMMARY));
        int prevResultSum = steps.extractSum(steps.onNewCalculatorPage().balance()
                .summary("Итог"));

        steps.addServiceInBlock(PREMIUM_SERVICE, steps.onNewCalculatorPage().usedCarsBlock());

        steps.shouldSeeAmountIncrease(previousBlockSum, steps.onNewCalculatorPage().usedCarsBlock().amount());
        steps.shouldSeeAmountIncrease(prevSummarySum, steps.onNewCalculatorPage().balance().summary("Легковые с"));
        steps.shouldSeeAmountIncrease(prevInCategorySum, steps.onNewCalculatorPage().balance()
                .summary(SERVICES_IN_CATEGORIES_SUMMARY));
        steps.shouldSeeAmountIncrease(prevResultSum, steps.onNewCalculatorPage().balance()
                .summary("Итог"));
    }

    @Ignore //поправить моки, добавить ответ ручки с отключённым тарифоим
    @Test
    @Category({Regression.class})
    @Owner(JENKL)
    @DisplayName("Количество звонков за период при отключённом тарифе")
    public void shouldSeeNumberOfCallsPerPeriodByDisabledTariff() {
        steps.onNewCalculatorPage().newCarsBlock().click();
        steps.expandOnlyAccommodationInCategoriesSummaryBlock();

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

}
