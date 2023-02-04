package ru.yandex.realty.wallet.www;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.UrlSteps;
import ru.yandex.realty.step.WalletSteps;

import static org.hamcrest.Matchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.Pages.WALLET;
import static ru.yandex.realty.consts.RealtyFeatures.CARDS;
import static ru.yandex.realty.element.wallet.Cards.ADD_CARD;
import static ru.yandex.realty.utils.AccountType.OWNER;

/**
 * @author kurau (Yuri Kalinin)
 */
@DisplayName("Привязка карты со страницы кошелька")
@Feature(CARDS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class AddCardFromWalletPageTest {

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
    private Account account;

    @Before
    public void openWallet() {
        apiSteps.createVos2Account(account, OWNER);
        urlSteps.testing().path(WALLET).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KURAU)
    @DisplayName("Проверяем, что дефолтный баланс «0»")
    public void shouldSeeDefaultBalance() {
        walletSteps.onWalletPage().balance().value().should(hasText("0 \u20BD"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KURAU)
    @DisplayName("Добавляем карту и «запоминаем»")
    public void shouldAddCardFromWallet() {
        walletSteps.onWalletPage().cards().button(ADD_CARD).click();
        walletSteps.payWithCardAndRemember();
        walletSteps.shouldSeeDefaultCard();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KURAU)
    @DisplayName("Пополняем счет, карту не «запоминаем»")
    public void shouldAddBalanceFromWallet() {
        walletSteps.addMoneyToWallet("2");
        walletSteps.payWithCardWithoutRemember();
        walletSteps.onWalletPage().cards().suggestButton().should(not(exists()));
        walletSteps.onWalletPage().cards().title().should(hasText("Добавьте карту для удобной оплаты"));
    }
}
