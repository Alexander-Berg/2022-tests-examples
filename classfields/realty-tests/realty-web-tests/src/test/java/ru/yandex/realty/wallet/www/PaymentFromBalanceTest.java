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
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.page.AuthorOfferPage;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.OfferPageSteps;
import ru.yandex.realty.step.PromocodesSteps;
import ru.yandex.realty.step.RetrofitApiSteps;
import ru.yandex.realty.step.UrlSteps;
import ru.yandex.realty.step.WalletSteps;

import java.io.IOException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.realty.consts.Owners.KOPITSA;
import static ru.yandex.realty.consts.Pages.OFFER;
import static ru.yandex.realty.consts.RealtyFeatures.WALLET;
import static ru.yandex.realty.page.AuthorOfferPage.ADD_VAS_FOR;
import static ru.yandex.realty.page.AuthorOfferPage.RISE_FOR;
import static ru.yandex.realty.utils.AccountType.OWNER;

/**
 * @author kurau (Yuri Kalinin)
 */
@DisplayName("Оплата услуг из кошелька")
@Feature(WALLET)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class PaymentFromBalanceTest {

    private static final String SUM = "1 000";
    private static final String INSUFFICIENT_SUM = "20";

    private String offerId;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private PromocodesSteps promoSteps;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private RetrofitApiSteps retrofitApiSteps;

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
        urlSteps.testing().path(Pages.WALLET).open();
    }

    @Test
    @Owner(KOPITSA)
    @DisplayName("Оплачиваем поднятие")
    public void shouldPayForRising() throws IOException {
        String expectedRaiseCost = retrofitApiSteps.getProductPrice("RAISING", account.getId(), offerId);
        walletSteps.addMoneyToWallet(SUM);
        walletSteps.payWithCardWithoutRemember();

        urlSteps.testing().path(OFFER).path(offerId).open();
        authorOfferPage.onAuthorOfferPage().vas(AuthorOfferPage.RAISING).button(RISE_FOR).click();
        walletSteps.payWithWallet();

        urlSteps.testing().path(Pages.WALLET).open();
        walletSteps.onWalletPage().transactionHistory().paymentList().should(hasSize(2));
        walletSteps.shouldSeeFirstTransaction("«Поднятие»", expectedRaiseCost);
    }

    @Test
    @Owner(KOPITSA)
    @DisplayName("Оплачиваем продвижение")
    public void shouldPayForPromotion() throws IOException {
        String expectedPromotionCost = retrofitApiSteps.getProductPrice("PROMOTION", account.getId(), offerId);
        walletSteps.addMoneyToWallet(SUM);
        walletSteps.payWithCardWithoutRemember();

        urlSteps.testing().path(OFFER).path(offerId).open();
        authorOfferPage.onAuthorOfferPage().vas(AuthorOfferPage.PROMOTION).button(ADD_VAS_FOR).click();
        walletSteps.payWithWallet();

        urlSteps.testing().path(Pages.WALLET).open();
        walletSteps.onWalletPage().transactionHistory().paymentList().should(hasSize(2));
        walletSteps.shouldSeeFirstTransaction("«Продвижение»", expectedPromotionCost);
    }

    @Test
    @Owner(KOPITSA)
    @DisplayName("Нет возможности оплаты кошельком если не хватает денег")
    public void shouldNotPayIfNotEnoughMoney() {
        walletSteps.addMoneyToWallet(INSUFFICIENT_SUM);
        walletSteps.payWithCardWithoutRemember();

        urlSteps.testing().path(OFFER).path(offerId).open();
        authorOfferPage.onAuthorOfferPage().vas(AuthorOfferPage.PREMIUM).button(ADD_VAS_FOR).click();

        walletSteps.switchToPaymentPopup();
        walletSteps.onWalletPage().cardsPopup().paymentMethods()
                .forEach(element -> element.should(not(hasText(containsString("Баланс кошелька")))));
    }
}
