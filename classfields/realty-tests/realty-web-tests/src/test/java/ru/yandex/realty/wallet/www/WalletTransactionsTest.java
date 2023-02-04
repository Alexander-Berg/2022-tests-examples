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
import ru.yandex.realty.consts.RealtyFeatures;
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

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.yandex.realty.adaptor.PromocodeAdaptor.defaultPromo;
import static ru.yandex.realty.adaptor.PromocodeAdaptor.promoFeature;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.OFFER;
import static ru.yandex.realty.consts.Pages.PROMOCODES;
import static ru.yandex.realty.consts.Pages.WALLET;
import static ru.yandex.realty.page.AuthorOfferPage.ADD_VAS_FOR;
import static ru.yandex.realty.page.AuthorOfferPage.ADD_VAS_FREE;
import static ru.yandex.realty.page.AuthorOfferPage.RISE_FOR;
import static ru.yandex.realty.utils.AccountType.OWNER;

/**
 * @author kurau (Yuri Kalinin)
 */

@DisplayName("Транзакции с кошелька")
@Feature(RealtyFeatures.WALLET)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class WalletTransactionsTest {

    private static final long DEFAULT_PROMO_BONUS = 3;

    private static final String SUM = "1000";

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
        offerId = offerBuildingSteps.addNewOffer(account).create().withSearcherWait().getId();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KANTEMIROV)
    @DisplayName("Видим транзакцию об оплате услуги с карты")
    public void shouldSeeCardPayForServiceTransaction() throws IOException {
        String expectedPromotionCost = retrofitApiSteps.getProductPrice("PROMOTION", account.getId(), offerId);
        urlSteps.testing().path(OFFER).path(offerId).open();
        authorOfferPage.onAuthorOfferPage().vas(AuthorOfferPage.PROMOTION).button(ADD_VAS_FOR).click();
        walletSteps.payWithCardWithoutRemember();

        urlSteps.testing().path(WALLET).open();
        walletSteps.onWalletPage().transactionHistory().paymentList().should(hasSize(1));
        walletSteps.shouldSeeFirstTransaction("«Продвижение»", expectedPromotionCost);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KANTEMIROV)
    @DisplayName("Видим транзакцию об оплате услуги с кошелька")
    public void shouldSeeBalancePayForServiceTransaction() throws IOException {
        String expectedRisingCost = retrofitApiSteps.getProductPrice("RAISING", account.getId(), offerId);
        urlSteps.testing().path(WALLET).open();
        walletSteps.addMoneyToWallet(SUM);
        walletSteps.payWithCardWithoutRemember();

        urlSteps.testing().path(OFFER).path(offerId).open();
        authorOfferPage.onAuthorOfferPage().vas(AuthorOfferPage.RAISING).button(RISE_FOR).click();
        walletSteps.payWithWallet();
        urlSteps.testing().path(WALLET).open();
        walletSteps.onWalletPage().transactionHistory().paymentList().should(hasSize(2));
        walletSteps.shouldSeeFirstTransaction("«Поднятие»", expectedRisingCost);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KANTEMIROV)
    @DisplayName("Видим транзакцию о пополнении баланса")
    public void shouldSeeAddBalanceTransaction() {
        urlSteps.testing().path(WALLET).open();
        walletSteps.addMoneyToWallet(SUM);
        walletSteps.payWithCardWithoutRemember();

        urlSteps.testing().path(WALLET).open();
        walletSteps.onWalletPage().transactionHistory().paymentList().should(hasSize(1));
        walletSteps.shouldSeeFirstTransaction("Пополнение кошелька", SUM);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KANTEMIROV)
    @DisplayName("Видим транзакцию об оплате промокодом")
    public void shouldSeePromoTransaction() throws IOException {
        String promoName = getRandomString();
        apiSteps.createPromocode(defaultPromo().withCode(promoName).withFeatures(singletonList(promoFeature()
                .withCount(DEFAULT_PROMO_BONUS)
                .withTag("promotion"))));

        urlSteps.testing().path(PROMOCODES).open();
        promoSteps.usePromoCode(promoName);

        urlSteps.testing().path(OFFER).path(offerId).open();
        authorOfferPage.onAuthorOfferPage().vas(AuthorOfferPage.PROMOTION).button(ADD_VAS_FREE).click();
        walletSteps.payAndWaitSuccess();
        apiSteps.waitBePromotedFirstOffer(account);

        urlSteps.testing().path(WALLET).open();
        walletSteps.onWalletPage().transactionHistory().paymentList()
                .should("Нет ототбразилась промо транзакция ", hasSize(1));
        String expectedPromotionCost = retrofitApiSteps.getProductPrice("PROMOTION", account.getId(), offerId);
        walletSteps.shouldSeeFirstTransaction("«Продвижение»", expectedPromotionCost);
    }
}


