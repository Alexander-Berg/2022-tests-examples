package ru.auto.tests.cabinet.calculator;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
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
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CalculatorPageSteps;
import ru.auto.tests.desktop.utils.Utils;

import javax.inject.Inject;

import static java.lang.String.format;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.AUTOSTRATEGY;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Notifications.AUTOSTRATEGY_DISABLED;
import static ru.auto.tests.desktop.consts.Notifications.AUTOSTRATEGY_ENABLED;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.CALCULATOR;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.element.cabinet.calculator.AutostrategyPopup.APPEND;
import static ru.auto.tests.desktop.element.cabinet.calculator.AutostrategyPopup.DISABLE;
import static ru.auto.tests.desktop.element.cabinet.calculator.AutostrategyPopup.PLACEHOLDER_TEMPLATE;
import static ru.auto.tests.desktop.element.cabinet.calculator.AutostrategyPopup.REDUCE;
import static ru.auto.tests.desktop.mock.MockAuctionStates.auctionStates;
import static ru.auto.tests.desktop.mock.MockAuctionStates.getBmw7ChangeAutostrategyRequest;
import static ru.auto.tests.desktop.mock.MockAuctionStates.getBmw7Context;
import static ru.auto.tests.desktop.mock.MockAuctionStates.getBmw7DeleteAutostrategyRequest;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.DEALER_AUCTION_AUTO_STRATEGY_CHANGE;
import static ru.auto.tests.desktop.mock.Paths.DEALER_AUCTION_AUTO_STRATEGY_DELETE;
import static ru.auto.tests.desktop.mock.Paths.DEALER_AUCTION_CURRENT_STATE;
import static ru.auto.tests.desktop.mock.beans.auctionState.AutoStrategy.autoStrategy;
import static ru.auto.tests.desktop.mock.beans.auctionState.AutoStrategySettings.settings;
import static ru.auto.tests.desktop.mock.beans.auctionState.State.state;
import static ru.auto.tests.desktop.step.CookieSteps.AUCTION_TOUR_STEP;
import static ru.auto.tests.desktop.step.CookieSteps.SHOWN;
import static ru.auto.tests.desktop.utils.Utils.formatPrice;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;

@Epic(CABINET_DEALER)
@Feature(AUTOSTRATEGY)
@Story("Автостратегия подключена")
@DisplayName("Теста на подключенную автостратегию")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class CalculatorAutostrategyActivatedTest {

    private static final int BASE_PRICE = 4800;
    private static final int MIN_BID = 5100;
    private static final int ONE_STEP = 100;
    private static final int MAX_BID = 6100;
    private static final String BMW_7ER = "BMW 7 серии";
    private static final String ACTIVE_AUTOSTRATEGY_POPUP_TEMPLATE = "Автостратегия\n" +
            "Выберите максимальную стоимость целевого звонка, и позиция в аукционе будет " +
            "автоматически максимизироваться в пределах указанного бюджета\n" +
            "Не менее %d ₽\n" +
            "Применить\n" +
            "Отключить";

    private final int minimumPossibleBid = BASE_PRICE + ONE_STEP;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private CalculatorPageSteps steps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("cabinet/SessionDirectDealerAristos"),
                stub("cabinet/ApiAccessClient"),
                stub("cabinet/CommonCustomerGetClientAristos"),
                stub("cabinet/DesktopClientsGetAristos"),
                stub("cabinet/DealerTariff/AllTariffs"),
                stub().withGetDeepEquals(DEALER_AUCTION_CURRENT_STATE)
                        .withResponseBody(auctionStates().setStates(
                                state().setContext(getBmw7Context())
                                        .setBasePrice(withKopecks(BASE_PRICE))
                                        .setOneStep(withKopecks(ONE_STEP))
                                        .setMinBid(withKopecks(MIN_BID))
                                        .setAutoStrategy(autoStrategy().setAutoStrategySettings(
                                                settings().setMaxBid(withKopecks(MAX_BID))
                                                        .setMaxPositionForPrice(new JsonObject())
                                        ))).getResponse())
        ).create();

        cookieSteps.setCookieForBaseDomain(AUCTION_TOUR_STEP, SHOWN);
        urlSteps.subdomain(SUBDOMAIN_CABINET).path(CALCULATOR).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кнопка активной автостратегии - зеленая")
    public void shouldSeeActiveAutostrategyButtonGreen() {
        steps.onNewCalculatorPage().newCarsBlock().auctionBlock().auction(BMW_7ER).autostrategyButton()
                .should(hasClass(containsString("_color_green")));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Текст попапа подключенной автостратегии")
    public void shouldSeeAutostrategyPopupText() {
        steps.onNewCalculatorPage().newCarsBlock().auctionBlock().auction(BMW_7ER).autostrategyButton().click();

        steps.onNewCalculatorPage().autostrategyPopup().should(hasText(
                format(ACTIVE_AUTOSTRATEGY_POPUP_TEMPLATE, minimumPossibleBid)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается сохраненная максимальная ставка автостратегии в инпуте")
    public void shouldSeeMaxBid() {
        steps.onNewCalculatorPage().newCarsBlock().auctionBlock().auction(BMW_7ER).autostrategyButton().click();

        steps.onNewCalculatorPage().autostrategyPopup().input().should(hasValue(formatPrice(MAX_BID)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кнопка «Применить» задизейблена без смены ставки")
    public void shouldSeeApplyDisabledWithoutBidChange() {
        steps.onNewCalculatorPage().newCarsBlock().auctionBlock().auction(BMW_7ER).autostrategyButton().click();

        steps.onNewCalculatorPage().autostrategyPopup().applyButton().should(not(isEnabled()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отключаем автостратегию")
    public void shouldSeeDisableAutostrategy() {
        steps.onNewCalculatorPage().newCarsBlock().auctionBlock().auction(BMW_7ER).autostrategyButton().click();

        mockRule.setStubs(
                stub().withPostDeepEquals(DEALER_AUCTION_AUTO_STRATEGY_DELETE)
                        .withRequestBody(getBmw7DeleteAutostrategyRequest())
                        .withStatusSuccessResponse()
        ).update();

        steps.onNewCalculatorPage().autostrategyPopup().button(DISABLE).click();

        steps.onNewCalculatorPage().notifier(AUTOSTRATEGY_DISABLED).should(isDisplayed());
        steps.onNewCalculatorPage().autostrategyPopup().should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Увеличиваем ставку с помощью «+», применяем автостратегию")
    public void shouldSeeAppendBidAndApplyAutostrategy() {
        steps.onNewCalculatorPage().newCarsBlock().auctionBlock().auction(BMW_7ER).autostrategyButton().click();
        steps.onNewCalculatorPage().autostrategyPopup().changeBid(APPEND).waitUntil(isDisplayed()).click();

        mockRule.setStubs(
                stub().withPostDeepEquals(DEALER_AUCTION_AUTO_STRATEGY_CHANGE)
                        .withRequestBody(getBmw7ChangeAutostrategyRequest(MAX_BID + ONE_STEP))
                        .withStatusSuccessResponse()
        ).update();

        steps.onNewCalculatorPage().autostrategyPopup().applyButton().click();

        steps.onNewCalculatorPage().notifier(AUTOSTRATEGY_ENABLED).should(isDisplayed());
        steps.onNewCalculatorPage().autostrategyPopup().should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Уменьшаем ставку с помощью «-», применяем автостратегию")
    public void shouldSeeReduceBidAndApplyAutostrategy() {
        steps.onNewCalculatorPage().newCarsBlock().auctionBlock().auction(BMW_7ER).autostrategyButton().click();
        steps.onNewCalculatorPage().autostrategyPopup().changeBid(REDUCE).waitUntil(isDisplayed()).click();

        mockRule.setStubs(
                stub().withPostDeepEquals(DEALER_AUCTION_AUTO_STRATEGY_CHANGE)
                        .withRequestBody(getBmw7ChangeAutostrategyRequest(MAX_BID - ONE_STEP))
                        .withStatusSuccessResponse()
        ).update();

        steps.onNewCalculatorPage().autostrategyPopup().applyButton().click();

        steps.onNewCalculatorPage().notifier(AUTOSTRATEGY_ENABLED).should(isDisplayed());
        steps.onNewCalculatorPage().autostrategyPopup().should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Меняем ставку через инпут, применяем автостратегию")
    public void shouldAddBidToInputAndApplyAutostrategy() {
        int changedBid = 7500;
        steps.onNewCalculatorPage().newCarsBlock().auctionBlock().auction(BMW_7ER).autostrategyButton().click();
        steps.onNewCalculatorPage().autostrategyPopup()
                .input(format(PLACEHOLDER_TEMPLATE, minimumPossibleBid), String.valueOf(changedBid));

        mockRule.setStubs(
                stub().withPostDeepEquals(DEALER_AUCTION_AUTO_STRATEGY_CHANGE)
                        .withRequestBody(getBmw7ChangeAutostrategyRequest(changedBid))
                        .withStatusSuccessResponse()
        ).update();

        steps.onNewCalculatorPage().autostrategyPopup().applyButton().click();

        steps.onNewCalculatorPage().notifier(AUTOSTRATEGY_ENABLED).should(isDisplayed());
        steps.onNewCalculatorPage().autostrategyPopup().should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кнопка «Применить» задизейблена при указании ставки ниже минимально возможной")
    public void shouldSeeApplyDisabledWithLessThanMinimumPossibleBid() {
        int tooLowBid = minimumPossibleBid - 1;
        steps.onNewCalculatorPage().newCarsBlock().auctionBlock().auction(BMW_7ER).autostrategyButton().click();
        steps.onNewCalculatorPage().autostrategyPopup()
                .input(format(PLACEHOLDER_TEMPLATE, minimumPossibleBid), String.valueOf(tooLowBid));

        steps.onNewCalculatorPage().autostrategyPopup().applyButton().should(not(isEnabled()));
    }

    private String withKopecks(int price) {
        return String.valueOf(price * 100);
    }

}
