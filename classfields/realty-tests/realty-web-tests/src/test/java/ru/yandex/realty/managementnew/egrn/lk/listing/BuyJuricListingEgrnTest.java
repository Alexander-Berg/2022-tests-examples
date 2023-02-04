package ru.yandex.realty.managementnew.egrn.lk.listing;

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
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.PromocodesSteps;
import ru.yandex.realty.step.UrlSteps;
import ru.yandex.realty.step.WalletSteps;
import ru.yandex.realty.utils.AccountType;

import static org.hamcrest.Matchers.hasSize;
import static ru.auto.test.api.realty.OfferType.APARTMENT_SELL;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.OfferByRegion.Region.EGRN_SUCCESS;
import static ru.yandex.realty.consts.OfferByRegion.getLocationForRegion;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_EGRN_REPORTS;
import static ru.yandex.realty.consts.Pages.OFFER;
import static ru.yandex.realty.consts.RealtyTags.JURICS;
import static ru.yandex.realty.element.offercard.EgrnBlock.BUY_FULL_REPORT;
import static ru.yandex.realty.step.CommonSteps.EGRN_TIMEOUT;

@Issue("VERTISTEST-1509")
@Tag(JURICS)
@DisplayName("Отчет ЕГРН. Карточка оффера")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class BuyJuricListingEgrnTest {

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
    private ManagementSteps managementSteps;

    @Inject
    private WalletSteps walletSteps;

    @Inject
    private PromocodesSteps promocodesSteps;

    @Before
    public void before() {
        apiSteps.createRealty3JuridicalAccount(account);
        apiSteps.createVos2AccountWithoutLogin(account2, AccountType.OWNER);
        String offerId = offerBuildingSteps.addNewOffer(account2)
                .withBody(OfferBuildingSteps.getDefaultOffer(APARTMENT_SELL)
                        .withLocation(getLocationForRegion(EGRN_SUCCESS))).create().getId();
        promocodesSteps.use2000Promo();
        urlSteps.testing().path(OFFER).path(offerId).open();
        basePageSteps.refreshUntil(() -> basePageSteps.onOfferCardPage().egrnBlock(), exists(), EGRN_TIMEOUT);
        basePageSteps.onOfferCardPage().egrnBlock().checkDataButton().click();
        basePageSteps.onOfferCardPage().egrnBlock().button(BUY_FULL_REPORT).waitUntil(isDisplayed()).click();
        walletSteps.onWalletPage().cardsPopup().waitUntil(isDisplayed(), 20);
        walletSteps.onWalletPage().cardsPopup().paymentButton().click();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Жмем «Купить полный отчет за» -> видим попап оплаты")
    public void shouldSeeReportOnOfferCard() {
        urlSteps.testing().path(MANAGEMENT_NEW_EGRN_REPORTS).open();
        basePageSteps.refreshUntil(() -> managementSteps.onEgrnListingPage().egrnSnippets(), hasSize(1));
    }
}
