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
import ru.yandex.realty.step.UrlSteps;
import ru.yandex.realty.step.WalletSteps;

import static org.hamcrest.Matchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KOPITSA;
import static ru.yandex.realty.consts.Pages.OFFER;
import static ru.yandex.realty.consts.Pages.WALLET;
import static ru.yandex.realty.page.AuthorOfferPage.ADD_VAS_FOR;
import static ru.yandex.realty.utils.AccountType.OWNER;

/**
 * @author kurau (Yuri Kalinin)
 */
@DisplayName("Приоритетность оплаты")
@Feature(RealtyFeatures.WALLET)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class PaymentPriorityTest {

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
        walletSteps.addMoneyToWallet("1 000");
        walletSteps.payWithCardWithoutRemember();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KOPITSA)
    @DisplayName("Приоритетная оплата с кошелька, если стоит галочка")
    public void shouldPrioritizePaymentFromBalance() {
        walletSteps.onWalletPage().balance().preferWalletCheckbox().click();
        urlSteps.testing().path(OFFER).path(offerId).open();
        authorOfferPage.onAuthorOfferPage().vas(AuthorOfferPage.PROMOTION).button(ADD_VAS_FOR).click();
        walletSteps.switchToPaymentPopup();
        walletSteps.onWalletPage().cardsPopup().walletContainer().should(isDisplayed());
        walletSteps.onWalletPage().cardsPopup().cardContainer().should(not(exists()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KOPITSA)
    @DisplayName("Приоритетная оплата с карты, если не стоит галочка")
    public void shouldPrioritizePaymentFromCard() {
        urlSteps.testing().path(OFFER).path(offerId).open();
        authorOfferPage.onAuthorOfferPage().vas(AuthorOfferPage.PROMOTION).button(ADD_VAS_FOR).click();
        walletSteps.switchToPaymentPopup();
        walletSteps.onWalletPage().cardsPopup().cardContainer().should(isDisplayed());
        walletSteps.onWalletPage().cardsPopup().walletContainer().should(not(exists()));
    }
}
