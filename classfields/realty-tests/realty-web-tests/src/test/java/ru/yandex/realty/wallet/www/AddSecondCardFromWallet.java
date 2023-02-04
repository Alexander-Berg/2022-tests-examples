package ru.yandex.realty.wallet.www;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.realty.consts.Pages.OFFER;
import static ru.yandex.realty.consts.Pages.WALLET;
import static ru.yandex.realty.consts.RealtyFeatures.CARDS;
import static ru.yandex.realty.element.wallet.Cards.ADD_CARD;
import static ru.yandex.realty.page.AuthorOfferPage.ADD_VAS_FOR;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.step.WalletSteps.SECOND_CARD_MASK;
import static ru.yandex.realty.utils.AccountType.OWNER;

@DisplayName("Привязка карты при оплате")
@Feature(CARDS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class AddSecondCardFromWallet {

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
    public void before() {
        apiSteps.createVos2Account(account, OWNER);
        urlSteps.testing().path(WALLET).open();
        walletSteps.onWalletPage().cards().button(ADD_CARD).click();
        walletSteps.payWithCardAndRemember();
        walletSteps.switchToPaymentPopup();
        walletSteps.addSecondCard();
    }

    @Test
    @DisplayName("Добавляем вторую карту")
    public void shouldAddSecondCard() {
        walletSteps.onWalletPage().cards().suggestButton().click();
        walletSteps.onWalletPage().cardList().waitUntil(hasSize(2));
        walletSteps.onWalletPage().cardList().filter(card -> card.getText().contains(SECOND_CARD_MASK))
                .should("Должна быть привязана вторая карта", hasSize(1));
    }

    @Test
    @DisplayName("Видим вторую карту при оплате")
    public void shouldSeeSecondCardWhenYouPay() {
        String offerId = offerBuildingSteps.addNewOffer(account).create().getId();
        walletSteps.switchToPaymentPopup();
        urlSteps.testing().path(OFFER).path(offerId).open();
        authorOfferPage.onAuthorOfferPage().vas(AuthorOfferPage.PREMIUM).button(ADD_VAS_FOR).click();
        walletSteps.onWalletPage().cardsPopup().paymentMethods().get(FIRST)
                .should(hasText(containsString(SECOND_CARD_MASK)));
    }

    @Test
    @DisplayName("При оплате должны выбирать карту с галочкой «Карта по умолчанию»")
    public void shouldSelectDefaultCard() {
        walletSteps.onWalletPage().cards().suggestButton().click();
        walletSteps.onWalletPage().cardList().waitUntil(hasSize(2)).get(1).click();
        walletSteps.onWalletPage().cards().mainCardCheckbox().click();
        String offerId = offerBuildingSteps.addNewOffer(account).create().getId();
        walletSteps.switchToPaymentPopup();
        urlSteps.testing().path(OFFER).path(offerId).open();
        authorOfferPage.onAuthorOfferPage().vas(AuthorOfferPage.PREMIUM).button(ADD_VAS_FOR).click();
        walletSteps.onWalletPage().cardsPopup().paymentMethods().get(FIRST)
                .should(hasText(containsString(walletSteps.cardNumberTemplate())));
    }
}
