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
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.OFFER;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.WALLET;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Кошелёк - списания по объявлениям")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class WalletExpensesByOffersTest {

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
        mockRule.newMock().with("cabinet/Session/DirectDealerMoscow",
                "cabinet/CommonCustomerGet",
                "cabinet/DealerAccount",
                "cabinet/DealerTariff/AllTariffs",
                "cabinet/CommonCustomerGet",
                "cabinet/ClientsGet",
                "cabinet/DealerCampaigns",
                "cabinet/ApiAccessClient",
                "cabinet/DealerWalletProductActivationsTotalStats",
                "cabinet/DealerWalletProductActivationsTotalOfferStats",
                "cabinet/DealerWalletProductDailyBalanceStats").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(WALLET).addParam("view_type", "offers").open();
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по объявлению")
    public void shouldClickOffer() {
        steps.onCabinetWalletPage().walletHistory().getOffer(0).title().waitUntil(isDisplayed()).click();
        urlSteps.switchToNextTab();
        urlSteps.testing().path(CARS).path(USED).path(SALE).path("/mazda/cx_5/1071919172-d2b82eae/")
                .shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке «Подробная статистика»")
    public void shouldClickStatsUrl() {
        steps.onCabinetWalletPage().walletHistory().getOffer(0).statsUrl().waitUntil(isDisplayed()).click();
        urlSteps.switchToNextTab();
        urlSteps.subdomain(SUBDOMAIN_CABINET).path(OFFER).path(CARS).path("/1071919172-d2b82eae/").shouldNotSeeDiff();
    }
}
