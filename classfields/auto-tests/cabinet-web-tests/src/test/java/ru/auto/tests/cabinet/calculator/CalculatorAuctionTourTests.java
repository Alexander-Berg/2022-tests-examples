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
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CalculatorPageSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.CALCULATOR;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Калькулятор. Аукцион, тур")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class CalculatorAuctionTourTests {

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
        mockRule.newMock().with("cabinet/SessionDirectDealerAristos",
                "cabinet/ApiAccessClient",
                "cabinet/CommonCustomerGetClientAristos",
                "cabinet/DesktopClientsGetAristos",
                "cabinet/DealerTariff/AllTariffs",
                "cabinet/DealerAuctionCurrentState",
                "cabinet/DealerAuctionPlaceBid",
                "cabinet/DealerAuctionLeave",
                "cabinet/DesktopSidebarGet").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(CALCULATOR).open();
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Повышаем ставку")
    public void shouldSeeAuctionTour() {
        steps.onNewCalculatorPage().newCarsBlock().auctionBlock().should(isDisplayed());
        steps.onNewCalculatorPage().auctionOnboardingPopup().text()
                .should(hasText("Ваша позиция в аукционе. Наведите на значение, чтобы посмотреть ваши ставки " +
                        "относительно конкурентов"));

        steps.onNewCalculatorPage().auctionOnboardingPopup().button("Далее").click();
        steps.onNewCalculatorPage().auctionOnboardingPopup().text()
                .should(hasText("Делайте ставки, чтобы принять участие в аукционе и управлять позицией. " +
                        "Больше ставка — выше позиция и количество звонков"));

        steps.onNewCalculatorPage().auctionOnboardingPopup().button("Далее").click();
        steps.onNewCalculatorPage().auctionOnboardingPopup().text()
                .should(hasText("Примените максимальную ставку, чтобы сразу занять первую позицию среди конкурентов"));

        steps.onNewCalculatorPage().auctionOnboardingPopup().button("Завершить").click();
        steps.onNewCalculatorPage().auctionOnboardingPopup().should(not(isDisplayed()));
    }
}
