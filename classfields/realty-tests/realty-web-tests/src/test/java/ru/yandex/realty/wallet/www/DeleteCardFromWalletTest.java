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
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.page.AuthorOfferPage;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.OfferPageSteps;
import ru.yandex.realty.step.UrlSteps;
import ru.yandex.realty.step.WalletSteps;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.Pages.OFFER;
import static ru.yandex.realty.consts.Pages.WALLET;
import static ru.yandex.realty.consts.RealtyFeatures.CARDS;
import static ru.yandex.realty.element.wallet.Cards.ADD_CARD;
import static ru.yandex.realty.page.AuthorOfferPage.ADD_VAS_FOR;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.utils.AccountType.OWNER;

/**
 * @author kurau (Yuri Kalinin)
 */
@DisplayName("Удаление существующей карты")
@Feature(CARDS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class DeleteCardFromWalletTest {

    private static final String SECOND_CARD = "5555555555554444";
    private static final String CARD_MASK = String.format("**** **** **** %s", SECOND_CARD.substring(12));

    private String offerId;

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

    @Inject
    private OfferBuildingSteps offerBuildingSteps;

    @Inject
    private OfferPageSteps authorOfferPage;

    @Before
    public void openWallet() {
        apiSteps.createVos2Account(account, OWNER);
        offerId = offerBuildingSteps.addNewOffer(account).create().getId();
        urlSteps.testing().path(WALLET).open();
        walletSteps.onWalletPage().cards().button(ADD_CARD).click();
        walletSteps.payWithCardAndRemember();
        walletSteps.switchToPaymentPopup();
    }

    @Test
    @Owner(KURAU)
    @DisplayName("Удаляем привязанную карту, не видим пр оплате услуг")
    public void shouldDeleteCardFromWallet() {
        walletSteps.onWalletPage().cards().deleteCardButton().click();
        walletSteps.onWalletPage().confirmDeleteCardButton().click();
        walletSteps.onWalletPage().cards().suggestButton().waitUntil(not(exists()));

        urlSteps.testing().path(OFFER).path(offerId).open();
        authorOfferPage.onAuthorOfferPage().vas(AuthorOfferPage.PREMIUM).button(ADD_VAS_FOR).click();

        walletSteps.onWalletPage().cardsPopup().paymentMethods().get(FIRST).should(hasText("Новой картой"));
        walletSteps.onWalletPage().cardsPopup().paymentMethods().get(1)
                .should(anyOf(hasText("ЮMoney"), hasText("Сбербанк Онлайн")));
    }

    @Test
    @DisplayName("Удаляем вторую добавленную главную карту")
    public void shouldDeleteNotMainCard() {
        walletSteps.addSecondCard();
        walletSteps.onWalletPage().cards().deleteCardButton().click();
        walletSteps.onWalletPage().confirmDeleteCardButton().click();
        walletSteps.shouldSeeDefaultCard();
    }

    @Test
    @DisplayName("Удаляем первую добавленную главную карту")
    public void shouldDeleteMainCardFromPopup() {
        walletSteps.addSecondCard();
        walletSteps.onWalletPage().cards().suggestButton().click();
        walletSteps.onWalletPage().cardList().waitUntil(hasSize(2)).get(1).click();
        walletSteps.onWalletPage().cards().deleteCardButton().click();
        walletSteps.onWalletPage().confirmDeleteCardButton().click();
        walletSteps.onWalletPage().cards().cardTypeMasterCard()
                .should(hasText(containsString(CARD_MASK)));
    }
}
