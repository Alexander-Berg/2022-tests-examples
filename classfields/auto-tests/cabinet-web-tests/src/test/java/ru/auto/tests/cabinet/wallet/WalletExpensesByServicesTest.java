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
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.BasePageSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.WALLET;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Кошелёк - списания по услугам")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class WalletExpensesByServicesTest {

    private static final String OFFER_ID = "/1076842087-f1e84/";

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

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthDealer",
                "cabinet/ApiAccessClient",
                "cabinet/CommonCustomerGet",
                "cabinet/DealerWalletProductActivationsDailyStats",
                "cabinet/DealerWalletProductActivationsTotalStats",
                "cabinet/DealerWalletProductPlacementActivationsOfferStats").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(WALLET).open();
    }

    @Test
    @Category({Regression.class, Screenshooter.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Итого за период в свернутом виде")
    public void shouldSeeTotalMinimized() {
        steps.onCabinetWalletPage().walletTotal().toggle().should(isDisplayed()).click();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetWalletPage().walletTotal(),
                        steps.onCabinetWalletPage().walletHistory(),
                        steps.onCabinetWalletPage().walletHistory().pager());

        cookieSteps.deleteCookie("isExpandedWalletTotal");
        urlSteps.setProduction().open();
        steps.onCabinetWalletPage().walletTotal().toggle().should(isDisplayed()).click();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetWalletPage().walletTotal(),
                        steps.onCabinetWalletPage().walletHistory(),
                        steps.onCabinetWalletPage().walletHistory().pager());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по пункту в подробной истории")
    public void shouldClickItem() {
        steps.onCabinetWalletPage().walletHistory().service("Размещение объявления").click();
        steps.onCabinetWalletPage().walletHistory().offerUrl().waitUntil(isDisplayed()).click();
        urlSteps.switchToNextTab();
        urlSteps.testing().path(CARS).path(USED).path(SALE).path("kia").path("ceed").path(OFFER_ID)
                .shouldNotSeeDiff();
    }
}
