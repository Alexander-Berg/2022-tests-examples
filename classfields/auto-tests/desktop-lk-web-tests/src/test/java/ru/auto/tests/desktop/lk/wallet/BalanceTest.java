package ru.auto.tests.desktop.lk.wallet;

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
import ru.auto.tests.desktop.categories.Billing;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.YaKassaSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LK;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.WALLET;
import static ru.auto.tests.desktop.step.CookieSteps.FORCE_DISABLE_TRUST;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Кошелёк - пополнение баланса")
@Feature(LK)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class BalanceTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private YaKassaSteps yaKassaSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() {
        cookieSteps.setExpFlags(FORCE_DISABLE_TRUST);

        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/User",
                "desktop-lk/BillingAutoruPaymentInitWalletBalance",
                "desktop-lk/BillingAutoruPaymentProcessWalletBalance",
                "desktop/BillingAutoruPayment").post();

        urlSteps.testing().path(MY).path(WALLET).open();

        mockRule.overwriteStub(1, "desktop-lk/UserWithBalance");
    }

    @Test
    @Category({Regression.class, Testing.class, Billing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Пополнение баланса")
    public void shouldAddMoneyToWallet() {
        basePageSteps.onWalletPage().walletBalance().button("Пополнить").click();
        basePageSteps.onWalletPage().switchToBillingFrame();
        basePageSteps.onWalletPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.onWalletPage().billingPopup().header().waitUntil(hasText("Пополнение кошелька"));
        basePageSteps.onWalletPage().billingPopup().priceHeader().waitUntil(hasText("1 000 ₽"));
        yaKassaSteps.payWithCard();
        yaKassaSteps.waitForSuccessMessage();
        basePageSteps.onLkSalesPage().billingPopupCloseButton().click();
        basePageSteps.onLkSalesPage().billingPopupFrame().waitUntil(not(isDisplayed()));
        basePageSteps.onWalletPage().walletBalance().balance().waitUntil(hasText("1 000 ₽"));
    }
}
