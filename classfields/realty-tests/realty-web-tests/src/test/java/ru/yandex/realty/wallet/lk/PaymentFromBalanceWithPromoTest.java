package ru.yandex.realty.wallet.lk;

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
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
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
import static ru.yandex.realty.consts.Owners.KOPITSA;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW;
import static ru.yandex.realty.consts.Pages.PROMOCODES;
import static ru.yandex.realty.consts.RealtyFeatures.WALLET;
import static ru.yandex.realty.element.management.OfferServicesPanel.ADD_SERVICE;
import static ru.yandex.realty.element.management.OfferServicesPanel.PROMOTION;
import static ru.yandex.realty.utils.AccountType.OWNER;

/**
 * @author kurau (Yuri Kalinin)
 */
@DisplayName("Оплата услуг из кошелька")
@Feature(WALLET)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class PaymentFromBalanceWithPromoTest {

    private static final long DEFAULT_PROMO_BONUS = 3;
    private static final String ONLY_FOR_PROMOTION = "promotion";
    private static final String SUM = "1 000";

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
    private ManagementSteps managementSteps;

    @Before
    public void openWallet() {
        apiSteps.createVos2Account(account, OWNER);
        offerId = offerBuildingSteps.addNewOffer(account).create().getId();
        urlSteps.testing().path(Pages.WALLET).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KOPITSA)
    @DisplayName("Оплачиваем премиум если есть промо")
    public void shouldPayForPromotionWithPromo() throws IOException {
        walletSteps.addMoneyToWallet(SUM);
        walletSteps.payWithCardWithoutRemember();
        String promoName = getRandomString();
        apiSteps.createPromocode(defaultPromo().withCode(promoName).withFeatures(singletonList(promoFeature()
                .withCount(DEFAULT_PROMO_BONUS).withTag(ONLY_FOR_PROMOTION))));

        urlSteps.testing().path(MANAGEMENT_NEW).path(PROMOCODES).open();
        promoSteps.usePromoCode(promoName);
        urlSteps.testing().path(MANAGEMENT_NEW).open();
        managementSteps.onManagementNewPage().offer(0).servicesPanel().service(PROMOTION).button(ADD_SERVICE)
                .click();
        walletSteps.payAndWaitSuccess();

        urlSteps.testing().path(Pages.WALLET).open();
        walletSteps.onWalletPage().transactionHistory().paymentList().should("Должны видеть 2 транзакции", hasSize(2));
        String expectedPromotionCost = retrofitApiSteps.getProductPrice("PROMOTION", account.getId(), offerId);
        walletSteps.shouldSeeFirstTransaction("«Продвижение»", expectedPromotionCost);
    }
}
