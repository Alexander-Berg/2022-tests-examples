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
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CalculatorPageSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Notifications.CALLS_LIMIT_SUCCESSFULY_UPDATED;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.CALCULATOR;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.element.cabinet.calculator.CalculatorCallsLimitsPopup.APPLY;
import static ru.auto.tests.desktop.element.cabinet.calculator.CalculatorCallsLimitsPopup.NO_LIMITS;
import static ru.auto.tests.desktop.mock.MockDealerTariff.newCarsCallsTariff;
import static ru.auto.tests.desktop.mock.MockDealerTariff.usedCarsCallsTariff;
import static ru.auto.tests.desktop.mock.MockDealerTariffs.dealerTariffs;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.DEALER_TARIFF;
import static ru.auto.tests.desktop.mock.Paths.SERVICE_BILLING_CAMPAIGN;
import static ru.auto.tests.desktop.mock.beans.stub.Query.query;
import static ru.auto.tests.desktop.utils.Utils.formatPrice;
import static ru.auto.tests.desktop.utils.Utils.getRandomBetween;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Калькулятор. Прямой дилер(Москва). Ограничение расходов.")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class CalculatorCallsLimitTest {

    private static final String CLIENT_ID = "16453";
    private static final String CALL_CLIENT = "call/client";
    private static final String CALL_CARS_USED_CLIENT = "call:cars:used/client";

    private final int dailyLimit = getRandomBetween(10000, 100000);

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private CalculatorPageSteps steps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("cabinet/Session/DirectDealerMoscow"),
                stub("cabinet/ApiAccessClient"),
                stub("cabinet/DesktopClientsGet/Dealer"),
                stub("cabinet/CommonCustomerGet")
        );

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(CALCULATOR);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение суммы ограничения расходов на легковых новых")
    public void shouldSeeNewCarsCallDailyLimit() {
        mockRule.setStubs(
                stub().withGetDeepEquals(DEALER_TARIFF)
                        .withResponseBody(
                                dealerTariffs().setTariffs(
                                        newCarsCallsTariff().setCallsDailyLimit(dailyLimit)
                                ).getBody())
        ).create();

        urlSteps.open();
        steps.onNewCalculatorPage().newCarsBlock().click();

        steps.onNewCalculatorPage().newCarsBlock().callsLimitBlock().limitInfo().should(hasText(
                format("Ограничение %s", formatPrice(dailyLimit))));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Смена суммы ограничения расходов на легковых новых")
    public void shouldSeeNewCarsCallDailyLimitPopupChange() {
        int newLimit = getRandomBetween(10000, 100000);

        mockRule.setStubs(
                stub().withGetDeepEquals(DEALER_TARIFF)
                        .withResponseBody(
                                dealerTariffs().setTariffs(
                                        newCarsCallsTariff().setCallsDailyLimit(dailyLimit)
                                ).getBody())
        ).create();

        urlSteps.open();
        steps.onNewCalculatorPage().newCarsBlock().click();
        steps.onNewCalculatorPage().newCarsBlock().callsLimitBlock().edit().click();

        steps.onNewCalculatorPage().callsLimitPopup().input().waitUntil(hasValue(formatPrice(dailyLimit)));

        mockRule.setStubs(
                stub().withPutDeepEquals(format("%s/%s/%s", SERVICE_BILLING_CAMPAIGN, CALL_CLIENT, CLIENT_ID))
                        .withRequestQuery(
                                query().setDayLimit(newLimit * 100).setEnabled(true).setRecalculateCostPerCall(true))
                        .withStatusSuccessResponse()
        ).update();

        steps.onNewCalculatorPage().callsLimitPopup().clearLimit().click();
        steps.onNewCalculatorPage().callsLimitPopup().input().sendKeys(String.valueOf(newLimit));
        steps.onNewCalculatorPage().callsLimitPopup().button(APPLY).click();

        steps.onNewCalculatorPage().notifier(CALLS_LIMIT_SUCCESSFULY_UPDATED).should(isDisplayed());
        steps.onNewCalculatorPage().callsLimitPopup().should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Смена суммы ограничения расходов на легковых новых на «Без ограничений»")
    public void shouldSeeNewCarsCallDailyLimitPopupChangeNoLimits() {
        mockRule.setStubs(
                stub().withGetDeepEquals(DEALER_TARIFF)
                        .withResponseBody(
                                dealerTariffs().setTariffs(
                                        newCarsCallsTariff().setCallsDailyLimit(dailyLimit)
                                ).getBody())
        ).create();

        urlSteps.open();
        steps.onNewCalculatorPage().newCarsBlock().click();
        steps.onNewCalculatorPage().newCarsBlock().callsLimitBlock().edit().click();

        steps.onNewCalculatorPage().callsLimitPopup().input().waitUntil(hasValue(formatPrice(dailyLimit)));

        mockRule.setStubs(
                stub().withPutDeepEquals(format("%s/%s/%s", SERVICE_BILLING_CAMPAIGN, CALL_CLIENT, CLIENT_ID))
                        .withRequestQuery(
                                query().setDayLimit(0).setEnabled(true).setRecalculateCostPerCall(true))
                        .withStatusSuccessResponse()
        ).update();

        steps.onNewCalculatorPage().callsLimitPopup().button(NO_LIMITS).click();

        steps.onNewCalculatorPage().notifier(CALLS_LIMIT_SUCCESSFULY_UPDATED).should(isDisplayed());
        steps.onNewCalculatorPage().callsLimitPopup().should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Очищаем сумму лимита расходов на легковых новых - «Применить» задизейблена")
    public void shouldClearNewCarsCallDailyLimitAndSeeApplyDisabled() {
        mockRule.setStubs(
                stub().withGetDeepEquals(DEALER_TARIFF)
                        .withResponseBody(
                                dealerTariffs().setTariffs(
                                        newCarsCallsTariff().setCallsDailyLimit(dailyLimit)
                                ).getBody())
        ).create();

        urlSteps.open();
        steps.onNewCalculatorPage().newCarsBlock().click();
        steps.onNewCalculatorPage().newCarsBlock().callsLimitBlock().edit().click();
        steps.onNewCalculatorPage().callsLimitPopup().clearLimit().click();

        steps.onNewCalculatorPage().callsLimitPopup().button(APPLY).should(not(isEnabled()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение суммы ограничения расходов на легковых б/у")
    public void shouldSeeUsedCarsCallDailyLimit() {
        mockRule.setStubs(
                stub().withGetDeepEquals(DEALER_TARIFF)
                        .withResponseBody(
                                dealerTariffs().setTariffs(
                                        usedCarsCallsTariff().setCallsDailyLimit(dailyLimit)
                                ).getBody())
        ).create();

        urlSteps.open();
        steps.onNewCalculatorPage().usedCarsBlock().click();

        steps.onNewCalculatorPage().usedCarsBlock().callsLimitBlock().limitInfo().should(hasText(
                format("Ограничение %s", formatPrice(dailyLimit))));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Смена суммы ограничения расходов на легковых б/у на «Без ограничений»")
    public void shouldSeeUsedCarsCallDailyLimitPopupChangeNoLimits() {
        mockRule.setStubs(
                stub().withGetDeepEquals(DEALER_TARIFF)
                        .withResponseBody(
                                dealerTariffs().setTariffs(
                                        usedCarsCallsTariff().setCallsDailyLimit(dailyLimit)
                                ).getBody())
        ).create();

        urlSteps.open();
        steps.onNewCalculatorPage().usedCarsBlock().click();
        steps.onNewCalculatorPage().usedCarsBlock().callsLimitBlock().edit().click();

        steps.onNewCalculatorPage().callsLimitPopup().input().waitUntil(hasValue(formatPrice(dailyLimit)));

        mockRule.setStubs(
                stub().withPutDeepEquals(format("%s/%s/%s", SERVICE_BILLING_CAMPAIGN, CALL_CARS_USED_CLIENT, CLIENT_ID))
                        .withRequestQuery(
                                query().setDayLimit(0).setEnabled(true).setRecalculateCostPerCall(true))
                        .withStatusSuccessResponse()
        ).update();

        steps.onNewCalculatorPage().callsLimitPopup().button(NO_LIMITS).click();

        steps.onNewCalculatorPage().notifier(CALLS_LIMIT_SUCCESSFULY_UPDATED).should(isDisplayed());
        steps.onNewCalculatorPage().callsLimitPopup().should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Смена суммы ограничения расходов на легковых б/у")
    public void shouldSeeUsedCarsCallDailyLimitPopupChange() {
        int newLimit = getRandomBetween(10000, 100000);

        mockRule.setStubs(
                stub().withGetDeepEquals(DEALER_TARIFF)
                        .withResponseBody(
                                dealerTariffs().setTariffs(
                                        usedCarsCallsTariff().setCallsDailyLimit(dailyLimit)
                                ).getBody())
        ).create();

        urlSteps.open();
        steps.onNewCalculatorPage().usedCarsBlock().click();
        steps.onNewCalculatorPage().usedCarsBlock().callsLimitBlock().edit().click();

        steps.onNewCalculatorPage().callsLimitPopup().input().waitUntil(hasValue(formatPrice(dailyLimit)));

        mockRule.setStubs(
                stub().withPutDeepEquals(format("%s/%s/%s", SERVICE_BILLING_CAMPAIGN, CALL_CARS_USED_CLIENT, CLIENT_ID))
                        .withRequestQuery(
                                query().setDayLimit(newLimit * 100).setEnabled(true).setRecalculateCostPerCall(true))
                        .withStatusSuccessResponse()
        ).update();

        steps.onNewCalculatorPage().callsLimitPopup().clearLimit().click();
        steps.onNewCalculatorPage().callsLimitPopup().input().sendKeys(String.valueOf(newLimit));
        steps.onNewCalculatorPage().callsLimitPopup().button(APPLY).click();

        steps.onNewCalculatorPage().notifier(CALLS_LIMIT_SUCCESSFULY_UPDATED).should(isDisplayed());
        steps.onNewCalculatorPage().callsLimitPopup().should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Очищаем сумму лимита расходов на легковых б/у - «Применить» задизейблена")
    public void shouldClearUsedCarsCallDailyLimitAndSeeApplyDisabled() {
        mockRule.setStubs(
                stub().withGetDeepEquals(DEALER_TARIFF)
                        .withResponseBody(
                                dealerTariffs().setTariffs(
                                        usedCarsCallsTariff().setCallsDailyLimit(dailyLimit)
                                ).getBody())
        ).create();

        urlSteps.open();
        steps.onNewCalculatorPage().usedCarsBlock().click();
        steps.onNewCalculatorPage().usedCarsBlock().callsLimitBlock().edit().click();
        steps.onNewCalculatorPage().callsLimitPopup().clearLimit().click();

        steps.onNewCalculatorPage().callsLimitPopup().button(APPLY).should(not(isEnabled()));
    }

}
