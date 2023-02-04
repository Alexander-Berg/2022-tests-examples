package ru.auto.tests.cabinet.wallet;

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
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.WALLET;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Кошелёк - график")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class WalletGraphTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps steps;

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
                "cabinet/ClientsGet",
                "cabinet/DealerCampaigns",
                "cabinet/DealerTariff",
                "cabinet/DealerWalletProductActivationsTotalStats",
                "cabinet/DealerWalletProductActivationsTotalOfferStats",
                "cabinet/DealerWalletProductActivationsDailyStats",
                "cabinet/DealerWalletDailyBalanceStats").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(WALLET).addParam("from", "2019-03-01")
                .addParam("to", "2019-05-31").open();
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Тултип")
    public void shouldSeeTooltip() {
        steps.onCabinetWalletPage().graph().hover();
        steps.onCabinetWalletPage().graph().tooltip().waitUntil(isDisplayed())
                .waitUntil(hasText("11 апреля, четверг\nИтого\n0 ₽"));
    }
}
