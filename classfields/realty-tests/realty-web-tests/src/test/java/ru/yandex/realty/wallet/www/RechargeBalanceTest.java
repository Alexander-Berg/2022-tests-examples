package ru.yandex.realty.wallet.www;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;
import ru.yandex.realty.step.WalletSteps;

import static java.lang.String.format;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.WALLET;
import static ru.yandex.realty.element.wallet.CardsPopup.FOLLOW_TO_PAYMENT;
import static ru.yandex.realty.element.wallet.CardsPopup.PAY;
import static ru.yandex.realty.utils.AccountType.OWNER;

/**
 * @author kurau (Yuri Kalinin)
 */
@DisplayName("Пополнение баланса кошелька")
@Feature(RealtyFeatures.WALLET)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class RechargeBalanceTest {

    private static int SUM = 100;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private WalletSteps walletSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private Account account;

    @Before
    public void openWallet() {
        apiSteps.createVos2Account(account, OWNER);
        urlSteps.testing().path(WALLET).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KANTEMIROV)
    @DisplayName("Пополняем с яндекс.денег")
    public void shouldRechargeFromYaMoney() {
        walletSteps.addMoneyToWallet(String.valueOf(SUM));
        walletSteps.onWalletPage().cardsPopup().paymentMethods().waitUntil(hasSize(greaterThan(0)))
                .filter(element -> element.getText().contains("ЮMoney")).should(hasSize(1)).get(0).click();
        walletSteps.onWalletPage().cardsPopup().button(FOLLOW_TO_PAYMENT).click();
        basePageSteps.waitUntilSeeTabsCount(2);
        basePageSteps.switchToNextTab();
        walletSteps.onWalletPage().button(PAY).click();
        walletSteps.switchToTab(0);
        walletSteps.onWalletPage().cardsPopup().successMessage().waitUntil("Платеж не прошёл", isDisplayed(), 55);
        walletSteps.onWalletPage().cardsPopup().close().clickIf(isDisplayed());
        walletSteps.onWalletPage().balance().value().should(hasText(format("%d \u20BD", SUM)));
    }

    @Ignore("Тест на пополнение кошелька со «Сбербанк онлайн»")
    @Test
    @DisplayName("Пополняем со сбера")
    public void shouldSeeRechargeFromSber() {
    }
}
