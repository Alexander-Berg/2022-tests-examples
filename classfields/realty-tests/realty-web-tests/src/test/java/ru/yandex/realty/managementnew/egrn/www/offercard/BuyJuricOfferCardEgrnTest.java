package ru.yandex.realty.managementnew.egrn.www.offercard;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.PromocodesSteps;
import ru.yandex.realty.step.UrlSteps;
import ru.yandex.realty.step.WalletSteps;
import ru.yandex.realty.utils.AccountType;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.test.api.realty.OfferType.APARTMENT_SELL;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.adaptor.PromocodeAdaptor.defaultPromo;
import static ru.yandex.realty.adaptor.PromocodeAdaptor.promoConstrains;
import static ru.yandex.realty.adaptor.PromocodeAdaptor.promoFeature;
import static ru.yandex.realty.consts.OfferByRegion.Region.EGRN_SUCCESS;
import static ru.yandex.realty.consts.OfferByRegion.getLocationForRegion;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.OFFER;
import static ru.yandex.realty.consts.RealtyTags.JURICS;
import static ru.yandex.realty.element.offercard.EgrnBlock.BUY_FULL_REPORT;
import static ru.yandex.realty.step.ApiSteps.ONLY_FOR_MONEY;
import static ru.yandex.realty.step.CommonSteps.EGRN_TIMEOUT;

@Issue("VERTISTEST-1509")
@Tag(JURICS)
@DisplayName("Отчет ЕГРН. Карточка оффера")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class BuyJuricOfferCardEgrnTest {

    private static final String MESSAGE = "На вашем счете недостаточно средств для покупки";

    private String offerId;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private Account account;

    @Inject
    private Account account2;

    @Inject
    private OfferBuildingSteps offerBuildingSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private WalletSteps walletSteps;

    @Inject
    private PromocodesSteps promocodesSteps;

    @Before
    public void before() {
        apiSteps.createRealty3JuridicalAccount(account);
        apiSteps.createVos2AccountWithoutLogin(account2, AccountType.OWNER);
        offerId = offerBuildingSteps.addNewOffer(account2)
                .withBody(OfferBuildingSteps.getDefaultOffer(APARTMENT_SELL)
                        .withLocation(getLocationForRegion(EGRN_SUCCESS))).create().getId();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Жмем «Купить полный отчет за» -> видим попап оплаты")
    public void shouldSeeReportOnOfferCardByFullPromo() {
        promocodesSteps.use2000Promo();
        urlSteps.testing().path(OFFER).path(offerId).open();
        basePageSteps.refreshUntil(() -> basePageSteps.onOfferCardPage().egrnBlock(), exists(), EGRN_TIMEOUT);
        basePageSteps.onOfferCardPage().egrnBlock().checkDataButton().click();
        basePageSteps.onOfferCardPage().egrnBlock().button(BUY_FULL_REPORT).waitUntil(isDisplayed()).click();
        walletSteps.onWalletPage().cardsPopup().waitUntil(isDisplayed(), 20);
        walletSteps.onWalletPage().cardsPopup().paymentButton().should(hasText("Подключить бесплатно")).click();
        walletSteps.onWalletPage().cardsPopup().spinVisible().waitUntil(not(isDisplayed()));
        urlSteps.open();
        basePageSteps.refreshUntil(() -> basePageSteps.onOfferCardPage().egrnBlock().purchasedReports(), hasSize(1));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Промокод на 100 рублей. Жмем «Купить полный отчет за» -> видим невозможность оплаты")
    public void shouldSeeReportOnOfferCard() {
        String firstCode = getRandomString();
        apiSteps.createPromocode(defaultPromo().withCode(firstCode).withFeatures(asList(promoFeature()
                .withCount(100L)
                .withTag(ONLY_FOR_MONEY)))
                .withConstraints(promoConstrains()));
        apiSteps.applyPromocode(firstCode, account.getId());
        urlSteps.testing().path(OFFER).path(offerId).open();
        basePageSteps.refreshUntil(() -> basePageSteps.onOfferCardPage().egrnBlock(), exists(), EGRN_TIMEOUT);
        basePageSteps.onOfferCardPage().egrnBlock().checkDataButton().click();
        basePageSteps.onOfferCardPage().egrnBlock().button(BUY_FULL_REPORT).waitUntil(isDisplayed()).click();
        walletSteps.onWalletPage().cardsPopup().should(hasText(containsString(MESSAGE)));
    }


    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Жмем «Купить полный отчет за» -> видим попап оплаты")
    public void shouldSeeBuyNotEnoughPopupForJuric() {
        urlSteps.testing().path(OFFER).path(offerId).open();
        basePageSteps.refreshUntil(() -> basePageSteps.onOfferCardPage().egrnBlock(), exists(), EGRN_TIMEOUT);
        basePageSteps.onOfferCardPage().egrnBlock().checkDataButton().click();
        basePageSteps.onOfferCardPage().egrnBlock().button(BUY_FULL_REPORT).waitUntil(isDisplayed()).click();
        walletSteps.onWalletPage().cardsPopup().should(hasText(containsString(MESSAGE)));
    }
}
