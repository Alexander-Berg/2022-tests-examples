package ru.auto.tests.cabinet.calculator;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.desktop.module.DesktopDevToolsTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CalculatorPageSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.AUTOSTRATEGY;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.CALCULATOR;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.matchers.RequestHasBodyMatcher.hasJsonBody;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneRequest;
import static ru.auto.tests.desktop.mock.MockAuctionStates.auctionStates;
import static ru.auto.tests.desktop.mock.MockAuctionStates.getBmwStateWithAutostrategy;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.DEALER_AUCTION_CURRENT_STATE;
import static ru.auto.tests.desktop.step.CookieSteps.AUCTION_TOUR_STEP;
import static ru.auto.tests.desktop.step.CookieSteps.SHOWN;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(CABINET_DEALER)
@Feature(AUTOSTRATEGY)
@Story("Автостратегия подключена")
@DisplayName("Проверяем запрос на удаление автостратегии, при снятии с аукциона")
@GuiceModules(DesktopDevToolsTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class CalculatorAutostrategyRemoveFromAuctionTest {

    private static final String BMW_7ER = "BMW 7 серии";

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

    @Inject
    private SeleniumMockSteps browserMockSteps;

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
                                        getBmwStateWithAutostrategy()
                                ).getResponse()))
                .create();

        cookieSteps.setCookieForBaseDomain(AUCTION_TOUR_STEP, SHOWN);
        urlSteps.subdomain(SUBDOMAIN_CABINET).path(CALCULATOR).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Проверяем запрос на удаление автостратегии, при снятии с аукциона")
    public void shouldSeeDeleteAutostrategyAfterRemoveFromAuction() {
        steps.onNewCalculatorPage().newCarsBlock().auctionBlock().auction(BMW_7ER).hover();
        steps.onNewCalculatorPage().newCarsBlock().auctionBlock().auction(BMW_7ER).leaveAuctionButton().click();
        steps.onNewCalculatorPage().newCarsBlock().auctionBlock().saveBetsButton().should(isDisplayed()).click();

        browserMockSteps.assertWithWaiting(onlyOneRequest(
                "/-/ajax/cabinet/postDealerAuctionAutoStrategyDelete/",
                hasJsonBody("request/DealerAuctionAutoStrategyDelete.json")
        ));
    }

}
