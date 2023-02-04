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
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LK;
import static ru.auto.tests.desktop.consts.Owners.KRISKOLU;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.WALLET;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Выбор карты для постоянной оплаты")
@Feature(LK)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class SelectDefaultCardTest {

    private static final String FIRST_CARD_NUMBER = "4111 11** **** 1111";
    private static final String SECOND_CARD_NUMBER = "4111 11** **** 1112";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop-lk/UserWithTiedCards",
                "desktop/BillingAutoruTiedCardsPut",
                "desktop-lk/BillingAutoruPaymentInitWalletTiedCard").post();

        urlSteps.testing().path(MY).path(WALLET).open();
    }

    @Test
    @Category({Regression.class, Testing.class, Billing.class})
    @Owner(KRISKOLU)
    @DisplayName("Выбор карты для постоянной оплаты в кошельке")
    public void shouldSelectDefaultCardInWallet() {
        basePageSteps.onWalletPage().tiedCards().select(FIRST_CARD_NUMBER).click();
        basePageSteps.onWalletPage().tiedCards().selectPopup().itemsList().should(hasSize(2));
        basePageSteps.onWalletPage().tiedCards().selectPopup().getItem(0).should(hasText(FIRST_CARD_NUMBER));
        basePageSteps.onWalletPage().tiedCards().selectPopup().getItem(1).should(hasText(SECOND_CARD_NUMBER));
        basePageSteps.onWalletPage().tiedCards().selectPopup().item(FIRST_CARD_NUMBER).click();
        basePageSteps.onWalletPage().tiedCards().checkbox().click();
        basePageSteps.onLkSalesPage().notifier().waitUntil(isDisplayed()).waitUntil(hasText("Теперь карта основная"));
    }

    @Test
    @Category({Regression.class, Testing.class, Billing.class})
    @Owner(KRISKOLU)
    @DisplayName("Выбор карты для постоянной оплаты в поп-апе оплаты")
    public void shouldSelectDefaultCardInBillingPopup() {
        basePageSteps.onWalletPage().walletBalance().button("Пополнить").click();
        basePageSteps.onWalletPage().switchToBillingFrame();
        basePageSteps.onWalletPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.onWalletPage().billingPopup().select(FIRST_CARD_NUMBER).should(isDisplayed());
        basePageSteps.onWalletPage().billingPopup().checkbox("Всегда оплачивать с этой карты").click();
        basePageSteps.onWalletPage().billingPopup()
                .checkboxChecked("Всегда оплачивать с этой карты").waitUntil(isDisplayed());

    }
}