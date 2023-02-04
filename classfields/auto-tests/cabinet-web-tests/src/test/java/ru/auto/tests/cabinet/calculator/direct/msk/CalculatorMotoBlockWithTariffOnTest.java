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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.element.cabinet.calculator.Balance;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CalculatorPageSteps;

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
@DisplayName("Кабинет дилера. Калькулятор. Прямой дилер(Москва). Мото")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class CalculatorMotoBlockWithTariffOnTest {

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

    @Before
    public void before() {
        mockRule.newMock().with("cabinet/Session/DirectDealerMoscow",
                        "cabinet/ApiAccessClient",
                        "cabinet/DealerTariff/MotoOn",
                        "cabinet/DesktopClientsGet/Dealer",
                        "cabinet/CommonCustomerGet")
                .post();
        urlSteps.subdomain(SUBDOMAIN_CABINET).path(CALCULATOR).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(JENKL)
    @DisplayName("Блок «Мото». Отключить тариф")
    public void shouldSeeTurnOfTariff() {
        steps.expandCalculatorAndBalanceBlocks(steps.onNewCalculatorPage().motoBlock());
        int prevAmount = steps.extractSum(steps.onNewCalculatorPage().motoBlock().amount());
        assertThat(prevAmount).isNotZero().describedAs("Проверяем что сумма размещения не равна нулю");
        steps.onNewCalculatorPage().balance().summary("Мото").should(isDisplayed());

        mockRule.delete();
        mockRule.newMock().with("cabinet/Session/DirectDealerMoscow",
                "cabinet/ApiAccessClient",
                "cabinet/DealerTariff/MotoOff",
                "cabinet/ApiServiceAutoruQuota").post();

        steps.onNewCalculatorPage().motoBlock().activeTariff().turnOffTariff().click();
        steps.onNewCalculatorPage().serviceStatusPopup("Ваш тариф успешно изменен").should(isDisplayed());
        int amount = steps.extractSum(steps.onNewCalculatorPage().motoBlock().amount());
        assertThat(amount).isZero().describedAs("Проверяем что сумма размещения равна нулю");
        steps.onNewCalculatorPage().balance().summary("Мото").should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(JENKL)
    @DisplayName("Блок «Мото». Услуги при включённом тарифе")
    public void shouldSeeServicesByActiveTariff() {
        steps.onNewCalculatorPage().motoBlock().click();
        steps.expandOnlyServicesInCategoriesSummaryBlock();
        int prevAmount = steps.extractSum(steps.onNewCalculatorPage().motoBlock().amount());
        int prevServInCat = steps.extractSum(steps.onNewCalculatorPage().balance().summary(Balance.SERVICES_IN_CATEGORIES_SUMMARY));
        int prevMoto = steps.extractSum(steps.onNewCalculatorPage().balance().summary("Мото"));
        int prevResult = steps.extractSum(steps.onNewCalculatorPage().balance().summary("Итог"));

        steps.addServiceInBlock(PREMIUM_SERVICE, steps.onNewCalculatorPage().motoBlock());

        steps.shouldSeeAmountIncrease(prevAmount, steps.onNewCalculatorPage().motoBlock().amount());
        steps.shouldSeeAmountIncrease(prevServInCat, steps.onNewCalculatorPage().balance()
                .summary(Balance.SERVICES_IN_CATEGORIES_SUMMARY));
        steps.shouldSeeAmountIncrease(prevMoto, steps.onNewCalculatorPage().balance().summary("Мото"));
        steps.shouldSeeAmountIncrease(prevResult, steps.onNewCalculatorPage().balance().summary("Итог"));
    }
}
