package ru.auto.tests.cabinet.tradein;

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
import ru.auto.tests.desktop.step.BasePageSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.TRADE_IN;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Трейд-ин - включение/выключение")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class TradeInTurnOnAndOffTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps steps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthDealer",
                "cabinet/ApiAccessClient",
                "cabinet/CommonCustomerGet",
                "cabinet/DealerAccount",
                "cabinet/ClientsGet",
                "cabinet/DealerTradeIn",
                "cabinet/DealerCampaignProducts",
                "cabinet/DealerCampaignProductTradeInRequestCarsUsedActivatePut",
                "cabinet/DealerCampaignProductTradeInRequestCarsUsedDelete").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(TRADE_IN).open();
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Включение/выключение трейд-ина")
    public void shouldTurnOnAndOff() {
        String buttonTitle = "Легковые с пробегом";

        steps.onCabinetTradeInPage().inactiveToggle(buttonTitle).should(isDisplayed()).click();
        steps.onCabinetTradeInPage().activeToggle(buttonTitle).waitUntil(isDisplayed()).click();
        steps.onCabinetTradeInPage().inactiveToggle(buttonTitle).waitUntil(isDisplayed());
    }
}
