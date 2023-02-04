package ru.yandex.realty.wallet.www;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
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
import ru.yandex.realty.page.AuthorOfferPage;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.OfferPageSteps;
import ru.yandex.realty.step.UrlSteps;
import ru.yandex.realty.step.WalletSteps;

import static org.hamcrest.CoreMatchers.containsString;
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
@DisplayName("Привязка карты при оплате")
@Feature(CARDS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class AddCardWithPaymentTest {

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
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KURAU)
    @DisplayName("Оплата картой")
    public void shouldPayWithCard() {
        urlSteps.testing().path(OFFER).path(offerId).open();
        authorOfferPage.onAuthorOfferPage().vas(AuthorOfferPage.PROMOTION).button(ADD_VAS_FOR).click();
        walletSteps.payWithCardAndRemember();
        apiSteps.waitBePromotedFirstOffer(account);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KURAU)
    @DisplayName("Привязываем карту при оплате")
    public void shouldAddCardAfterPayment() {
        buyPremiumForOffer();
        walletSteps.payWithCardAndRemember();
        urlSteps.testing().path(WALLET).open();
        walletSteps.shouldSeeDefaultCard();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KURAU)
    @DisplayName("Подставляем существующую карту при оплате")
    public void shouldSuggestExistsCardWhenYouPay() {
        urlSteps.testing().path(WALLET).open();
        walletSteps.onWalletPage().cards().button(ADD_CARD).click();
        walletSteps.payWithCardAndRemember();

        buyPremiumForOffer();

        walletSteps.switchToPaymentPopup();
        walletSteps.onWalletPage().cardsPopup().paymentMethods().get(FIRST)
                .should(hasText(containsString(walletSteps.cardNumberTemplate())));
    }

    @Step("Покупаем «Премиум»")
    private void buyPremiumForOffer() {
        urlSteps.testing().path(OFFER).path(offerId).open();
        authorOfferPage.onAuthorOfferPage().vas(AuthorOfferPage.PREMIUM).button(ADD_VAS_FOR).click();
    }
}
