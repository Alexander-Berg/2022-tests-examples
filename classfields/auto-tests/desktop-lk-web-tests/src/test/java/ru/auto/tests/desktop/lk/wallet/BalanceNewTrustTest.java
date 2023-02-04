package ru.auto.tests.desktop.lk.wallet;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.desktop.categories.Billing;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopDevToolsTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.NewTrustSteps;
import ru.auto.tests.desktop.step.PublicApiSteps;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LK;
import static ru.auto.tests.desktop.consts.AutoruFeatures.NEW_TRUST;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.WALLET;
import static ru.auto.tests.desktop.element.BillingPopup.CONTINUE_PURCHASE;
import static ru.auto.tests.desktop.element.BillingPopup.EXIT;
import static ru.auto.tests.desktop.element.lk.WalletBalanceBlock.DEPOSIT;
import static ru.auto.tests.desktop.mock.MockBillingPaymentInitRequest.paymentInitWalletRequest;
import static ru.auto.tests.desktop.mock.MockBillingPaymentInitResponse.paymentInitWalletResponse;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.BILLING_AUTORU_PAYMENT_INIT;
import static ru.auto.tests.desktop.step.CookieSteps.FORCE_IGNORE_TRUST_EXP_RESULT;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Кошелёк - пополнение баланса через новый траст")
@Feature(LK)
@Story(NEW_TRUST)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopDevToolsTestsModule.class)
public class BalanceNewTrustTest {

    private static final String BALANCE = "1 000 ₽";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private NewTrustSteps newTrustSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Inject
    private PublicApiSteps publicApiSteps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private SeleniumMockSteps seleniumMockSteps;

    @Before
    public void before() {
        cookieSteps.setExpFlags(FORCE_IGNORE_TRUST_EXP_RESULT);

        seleniumMockSteps.setNewTrustBillingBrowserMock();

        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/User"),
                stub("desktop-lk/NewTrustStart"),
                stub("desktop-lk/NewTrustPayment"),

                stub().withPostDeepEquals(BILLING_AUTORU_PAYMENT_INIT)
                        .withRequestBody(
                                paymentInitWalletRequest().getBody())
                        .withResponseBody(
                                paymentInitWalletResponse().getBody())
        ).create();

        urlSteps.testing().path(MY).path(WALLET).open();

        mockRule.overwriteStub(1,
                stub("desktop-lk/UserWithBalance")
        );

        basePageSteps.onWalletPage().walletBalance().button(DEPOSIT).waitUntil(isDisplayed()).click();
        basePageSteps.onWalletPage().switchToBillingFrame();
        basePageSteps.onWalletPage().billingPopup().waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class, Billing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Пополнение баланса. Новый траст.")
    public void shouldAddMoneyToWallet() {
        basePageSteps.onWalletPage().newTrust().title().waitUntil(hasText("Пополнение кошелька"), 15);
        basePageSteps.onWalletPage().newTrust().price().waitUntil(hasText(BALANCE));

        newTrustSteps.payWithCard();
        newTrustSteps.waitForSuccessMessage();

        basePageSteps.onLkSalesPage().billingPopupCloseButton().waitUntil(isDisplayed()).click();
        basePageSteps.onLkSalesPage().billingPopupFrame().waitUntil(not(isDisplayed()));
        basePageSteps.onWalletPage().walletBalance().balance().waitUntil(hasText(BALANCE));
    }


    @Test
    @Category({Regression.class, Testing.class, Billing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Пополнение баланса. Закрываем попап в начале. Новый траст.")
    public void shouldCloseWallet() {
        basePageSteps.switchToDefaultFrame();
        basePageSteps.onLkSalesPage().billingPopupCloseButton().waitUntil(isDisplayed()).click();

        basePageSteps.onWalletPage().switchToBillingFrame();
        basePageSteps.onLkSalesPage().billingPopup().button(EXIT).click();

        basePageSteps.onLkSalesPage().billingPopupFrame().waitUntil(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class, Billing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Пополнение баланса, жмём на закрытие попапа, жмем «Продолжить покупку», пополняем. Новый траст.")
    public void shouldContinuePurchaseAfterCancelWallet() {
        basePageSteps.switchToDefaultFrame();
        basePageSteps.onLkSalesPage().billingPopupCloseButton().waitUntil(isDisplayed()).click();

        basePageSteps.onWalletPage().switchToBillingFrame();
        basePageSteps.onLkSalesPage().billingPopup().button(CONTINUE_PURCHASE).click();

        basePageSteps.onWalletPage().newTrust().title().waitUntil(hasText("Пополнение кошелька"), 15);
        basePageSteps.onWalletPage().newTrust().price().waitUntil(hasText(BALANCE));

        newTrustSteps.payWithCard();
        newTrustSteps.waitForSuccessMessage();

        basePageSteps.onLkSalesPage().billingPopupCloseButton().waitUntil(isDisplayed()).click();
        basePageSteps.onLkSalesPage().billingPopupFrame().waitUntil(not(isDisplayed()));
        basePageSteps.onWalletPage().walletBalance().balance().waitUntil(hasText(BALANCE));
    }

}
