package ru.yandex.realty.card;

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
import ru.yandex.realty.module.RealtyWebWithPhoneModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.PromocodesSteps;
import ru.yandex.realty.step.UrlSteps;
import ru.yandex.realty.step.WalletSteps;

import static org.hamcrest.core.IsNot.not;
import static ru.auto.test.api.realty.OfferType.APARTMENT_SELL;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.Pages.OFFER;
import static ru.yandex.realty.consts.RealtyFeatures.MANAGEMENT_NEW;
import static ru.yandex.realty.element.offers.ServicePayment.ACTIVATE;
import static ru.yandex.realty.element.offers.ServicePayment.PROMOTION;
import static ru.yandex.realty.matchers.OfferInfoMatchers.hasPromotion;
import static ru.yandex.realty.utils.AccountType.OWNER;

/**
 * @author kurau (Yuri Kalinin)
 */

@DisplayName("Продвижение со страницы оффера")
@Feature(MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithPhoneModule.class)
public class PromotionEnableTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private PromocodesSteps promocodesSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private Account account;

    @Inject
    private WalletSteps walletSteps;

    @Inject
    private OfferBuildingSteps createOfferSteps;

    @Inject
    private BasePageSteps basePageSteps;


    @Before
    public void openWallet() {
        apiSteps.createVos2Account(account, OWNER);
        String offerId =
                createOfferSteps.addNewOffer(account).withType(APARTMENT_SELL).create().getId();
        promocodesSteps.use2000Promo();
        urlSteps.testing().path(OFFER).path(offerId).open();
        basePageSteps.refreshUntil(() ->
                basePageSteps.onOfferCardPage().servicePayment().service(PROMOTION).button(ACTIVATE), isDisplayed());
        basePageSteps.onOfferCardPage().servicePayment().service(PROMOTION).button(ACTIVATE).click();
    }

    @Test
    @Owner(KURAU)
    @DisplayName("Включаем «продвижение» после публикации с оплатой")
    public void shouldSeePromotionAfterPayment() {
        walletSteps.payAndWaitSuccess();
        apiSteps.waitFirstOffer(account, hasPromotion());
    }

    @Test
    @Owner(KURAU)
    @DisplayName("Не включаем «продвижение» после отмены оплаты")
    public void shouldNotSeePromotionWithoutPay() {
        walletSteps.onWalletPage().promocodePopup().waitUntil(isDisplayed());
        walletSteps.onWalletPage().promocodePopup().closeButton().click();
        apiSteps.waitFirstOffer(account, not(hasPromotion()));
    }
}
