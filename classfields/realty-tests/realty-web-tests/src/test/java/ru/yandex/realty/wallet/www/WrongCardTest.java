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
import ru.yandex.realty.anno.ProfsearchAccount;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.module.RealtyWebModuleWithoutDelete;
import ru.yandex.realty.step.PassportSteps;
import ru.yandex.realty.step.UrlSteps;
import ru.yandex.realty.step.WalletSteps;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.WALLET;
import static ru.yandex.realty.consts.RealtyFeatures.CARDS;

/**
 * @author kurau (Yuri Kalinin)
 */
@DisplayName("Сообщения об ошибках для неправильной карты")
@Feature(CARDS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModuleWithoutDelete.class)
public class WrongCardTest {

    private final static String MONTH = "01";
    private final static String YEAR = "23";
    private final static String WRONG_YEAR = "17";
    private final static String WRONG_CARD_NUMBER = "0";
    private final static String CVC = "000";
    private final static String SHORT_CVC = "0";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private PassportSteps passportSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private WalletSteps walletSteps;

    @ProfsearchAccount
    @Inject
    private Account account;

    @Before
    public void openWallet() {
        passportSteps.login(account);
        urlSteps.testing().path(WALLET).open();
        walletSteps.addMoneyToWallet("100");
        walletSteps.switchToCardForm();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KANTEMIROV)
    @DisplayName("Видим ошибку о неправильном номере карты и не оплачиваем")
    public void shouldSeeWrongNumberError() {
        walletSteps.onWalletPage().addCardForm().cardNumber().waitUntil("", exists(), 60).sendKeys(WRONG_CARD_NUMBER);
        walletSteps.onWalletPage().addCardForm().month().sendKeys(MONTH);
        walletSteps.onWalletPage().addCardForm().year().sendKeys(YEAR);
        walletSteps.onWalletPage().addCardForm().cardCvc().sendKeys(CVC);
        payClickWithSwitch();
        walletSteps.onWalletPage().errors().should(hasSize(1)).should(hasItem(hasText("Карты с таким номером нет")));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KANTEMIROV)
    @DisplayName("Видим ошибку о неправильном размере CVC карты и не оплачиваем")
    public void shouldSeeWrongCVCError() {
        walletSteps.onWalletPage().addCardForm().cardNumber().waitUntil("", exists(), 60)
                .sendKeys(walletSteps.cardNumber());
        walletSteps.onWalletPage().addCardForm().month().sendKeys(MONTH);
        walletSteps.onWalletPage().addCardForm().year().sendKeys(YEAR);
        walletSteps.onWalletPage().addCardForm().cardCvc().sendKeys(SHORT_CVC);
        payClickWithSwitch();
        walletSteps.onWalletPage().errors().should(hasSize(1)).should(hasItem(hasText("Код не подходит")));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KANTEMIROV)
    @DisplayName("Видим ошибку о том, что дата карты уже прошла, и не оплачиваем")
    public void shouldSeeWrongYearError() {
        walletSteps.onWalletPage().addCardForm().cardNumber().waitUntil("", exists(), 60)
                .sendKeys(walletSteps.cardNumber());
        walletSteps.onWalletPage().addCardForm().month().sendKeys(MONTH);
        walletSteps.onWalletPage().addCardForm().year().sendKeys(WRONG_YEAR);
        walletSteps.onWalletPage().addCardForm().cardCvc().sendKeys(CVC);
        payClickWithSwitch();
        walletSteps.onWalletPage().errors().should(hasSize(1)).should(hasItem(hasText("Дата не подходит")));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KANTEMIROV)
    @DisplayName("Видим ошибку о том, что не заполнили номер карты, и не оплачиваем")
    public void shouldSeeNeedToFillCardNumberError() {
        walletSteps.onWalletPage().addCardForm().month().sendKeys(MONTH);
        walletSteps.onWalletPage().addCardForm().year().sendKeys(YEAR);
        walletSteps.onWalletPage().addCardForm().cardCvc().sendKeys(CVC);
        payClickWithSwitch();
        walletSteps.onWalletPage().errors().should(hasSize(1)).should(hasItem(hasText("Укажите номер карты")));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KANTEMIROV)
    @DisplayName("Видим ошибку о том, что не заполнили CVC, и не оплачиваем")
    public void shouldSeeNeedToFillCvcError() {
        walletSteps.onWalletPage().addCardForm().cardNumber().waitUntil("", exists(), 60)
                .sendKeys(walletSteps.cardNumber());
        walletSteps.onWalletPage().addCardForm().month().sendKeys(MONTH);
        walletSteps.onWalletPage().addCardForm().year().sendKeys(YEAR);
        payClickWithSwitch();
        walletSteps.onWalletPage().errors().should(hasSize(1)).should(hasItem(hasText("Укажите код (3 цифры)")));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KANTEMIROV)
    @DisplayName("Видим ошибку о том, что не заполнили год, и не оплачиваем")
    public void shouldSeeNeedToFillYearError() {
        walletSteps.onWalletPage().addCardForm().cardNumber().waitUntil("", exists(), 60)
                .sendKeys(walletSteps.cardNumber());
        walletSteps.onWalletPage().addCardForm().cardCvc().sendKeys(CVC);
        payClickWithSwitch();
        walletSteps.onWalletPage().errors().should(hasSize(1)).should(hasItem(hasText("Укажите год")));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KANTEMIROV)
    @DisplayName("Видим ошибку о том, что не заполнили месяц, и не оплачиваем")
    public void shouldSeeNeedToFillMonthError() {
        walletSteps.onWalletPage().addCardForm().cardNumber().waitUntil("", exists(), 60)
                .sendKeys(walletSteps.cardNumber());
        walletSteps.onWalletPage().addCardForm().month().sendKeys(YEAR);
        walletSteps.onWalletPage().addCardForm().cardCvc().sendKeys(CVC);
        payClickWithSwitch();
        walletSteps.onWalletPage().errors().should(hasSize(1)).should(hasItem(hasText("Укажите год")));
    }

    private void payClickWithSwitch() {
        walletSteps.switchToPaymentPopup();
        walletSteps.onWalletPage().cardsPopup().paymentButton().click();
        walletSteps.switchToCardForm();
    }
}
