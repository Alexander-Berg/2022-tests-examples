package ru.auto.tests.cabinet.calculator;

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
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CalculatorPageSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.CALCULATOR;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Калькулятор. Аукцион")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class CalculatorAuctionTests {

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
    public MockRule mockRule;

    @Before
    public void before() {
        mockRule.newMock().with("cabinet/SessionDirectDealerAristos",
                "cabinet/ApiAccessClient",
                "cabinet/CommonCustomerGetClientAristos",
                "cabinet/DesktopClientsGetAristos",
                "cabinet/DealerTariff/AllTariffs",
                "cabinet/DealerAuctionCurrentState",
                "cabinet/DealerAuctionPlaceBid",
                "cabinet/DealerAuctionLeave").post();

        cookieSteps.setCookieForBaseDomain("is_showing_onboarding_auction", "SHOWN");
        urlSteps.subdomain(SUBDOMAIN_CABINET).path(CALCULATOR).open();
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Повышаем ставку")
    public void shouldPlusBet() {
        steps.onNewCalculatorPage().newCarsBlock().auctionBlock().should(isDisplayed());
        steps.onNewCalculatorPage().newCarsBlock().auctionBlock().auction("BMW 5 серии").currentBet().hover();
        steps.onNewCalculatorPage().newCarsBlock().auctionBlock().auction("BMW 5 серии").plusBetButton().hover()
                .click();
        steps.onNewCalculatorPage().newCarsBlock().auctionBlock().auction("BMW 5 серии").currentPosition()
                .should(hasText("2-3 / 3"));
        steps.onNewCalculatorPage().newCarsBlock().auctionBlock().saveBetsButton().should(isDisplayed()).click();
        steps.onNewCalculatorPage().notifier().waitUntil(isDisplayed()).should(hasText("Данные успешно сохранены"));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Понижаем ставку")
    public void shouldMinusBet() {
        steps.onNewCalculatorPage().newCarsBlock().auctionBlock().should(isDisplayed());
        steps.onNewCalculatorPage().newCarsBlock().auctionBlock().auction("Cadillac SRX").currentBet().hover();
        steps.onNewCalculatorPage().newCarsBlock().auctionBlock().auction("Cadillac SRX").minusBetButton()
                .hover().click();
        steps.onNewCalculatorPage().newCarsBlock().auctionBlock().auction("Cadillac SRX").currentPosition()
                .should(hasText("1-2 / 2"));
        steps.onNewCalculatorPage().newCarsBlock().auctionBlock().saveBetsButton().should(isDisplayed()).click();
        steps.onNewCalculatorPage().notifier().waitUntil(isDisplayed()).should(hasText("Данные успешно сохранены"));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Покидаем аукцион")
    public void shouldLeaveAuction() {
        steps.onNewCalculatorPage().newCarsBlock().auctionBlock().should(isDisplayed());
        steps.onNewCalculatorPage().newCarsBlock().auctionBlock().auction("BMW 5 серии").currentBet().hover();
        steps.onNewCalculatorPage().newCarsBlock().auctionBlock().auction("BMW 5 серии").leaveAuctionButton()
                .hover().click();
        steps.onNewCalculatorPage().newCarsBlock().auctionBlock().auction("BMW 5 серии").currentPosition()
                .should(hasText("- / 2"));
        steps.onNewCalculatorPage().newCarsBlock().auctionBlock().saveBetsButton().should(isDisplayed()).click();
        steps.onNewCalculatorPage().notifier().waitUntil(isDisplayed()).should(hasText("Данные успешно сохранены"));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Применяем ставку первого места")
    public void shouldApplyFirstPlaceBet() {
        steps.onNewCalculatorPage().newCarsBlock().auctionBlock().should(isDisplayed());
        steps.onNewCalculatorPage().newCarsBlock().auctionBlock().auction("BMW 5 серии").firstPlaceBet().hover();
        steps.onNewCalculatorPage().newCarsBlock().auctionBlock().auction("BMW 5 серии").firstPlaceBet()
                .button().hover().click();
        steps.onNewCalculatorPage().newCarsBlock().auctionBlock().auction("BMW 5 серии").currentPosition()
                .should(hasText("1 / 3"));
        steps.onNewCalculatorPage().newCarsBlock().auctionBlock().saveBetsButton().should(isDisplayed()).click();
        steps.onNewCalculatorPage().notifier().waitUntil(isDisplayed()).should(hasText("Данные успешно сохранены"));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Фильтр по марке")
    public void shouldFilterByMark() {
        steps.onNewCalculatorPage().newCarsBlock().auctionBlock().should(isDisplayed());
        steps.onNewCalculatorPage().newCarsBlock().auctionBlock().auctionsList().should(hasSize(3));
        steps.onNewCalculatorPage().newCarsBlock().auctionBlock().auctionFilters().filter("BMW").click();
        steps.onNewCalculatorPage().newCarsBlock().auctionBlock().auctionsList().should(hasSize(1));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Фильтр только в аукционе")
    public void shouldFilterOnlyAuction() {
        steps.onNewCalculatorPage().newCarsBlock().auctionBlock().should(isDisplayed());
        steps.onNewCalculatorPage().newCarsBlock().auctionBlock().auctionsList().should(hasSize(3));
        steps.onNewCalculatorPage().newCarsBlock().auctionBlock().auctionFilters().auctionCheckbox().click();
        steps.onNewCalculatorPage().newCarsBlock().auctionBlock().auctionsList().should(hasSize(2));

        steps.onNewCalculatorPage().newCarsBlock().auctionBlock().auction("BMW 5 серии").currentPosition()
                .should(hasText("3 / 3"));
        steps.onNewCalculatorPage().newCarsBlock().auctionBlock().auction("Cadillac SRX").currentPosition()
                .should(hasText("1 / 2"));
    }
}
