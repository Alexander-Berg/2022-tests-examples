package ru.yandex.realty.managementnew.egrn.www.offercard;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.test.api.realty.OfferType.APARTMENT_SELL;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.OfferByRegion.Region.EGRN_SUCCESS;
import static ru.yandex.realty.consts.OfferByRegion.getLocationForRegion;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_EGRN_REPORTS;
import static ru.yandex.realty.consts.Pages.OFFER;
import static ru.yandex.realty.consts.Pages.WALLET;
import static ru.yandex.realty.element.offercard.EgrnBlock.BUY_FULL_REPORT;
import static ru.yandex.realty.element.offercard.EgrnBlock.SEE_BUTTON;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;
import static ru.yandex.realty.step.CommonSteps.EGRN_TIMEOUT;

@Issue("VERTISTEST-1509")
@DisplayName("Отчет ЕГРН. Карточка оффера")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class BuyOfferCardEgrnTest {

    private static final String EGRN_REPORT = "/egrn-report/";
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
        apiSteps.createVos2Account(account, AccountType.OWNER);
        apiSteps.createVos2AccountWithoutLogin(account2, AccountType.OWNER);
        String offerId = offerBuildingSteps.addNewOffer(account2)
                .withBody(OfferBuildingSteps.getDefaultOffer(APARTMENT_SELL)
                        .withLocation(getLocationForRegion(EGRN_SUCCESS))).create().getId();
        promocodesSteps.use2000Promo();
        urlSteps.testing().path(OFFER).path(offerId).open();
        basePageSteps.refreshUntil(() -> basePageSteps.onOfferCardPage().egrnBlock(), exists(), EGRN_TIMEOUT);
        basePageSteps.onOfferCardPage().egrnBlock().checkDataButton().click();
        clickBuy();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Покупаем отчет -> появляется «Смотреть» на карточке оффера")
    public void shouldSeeWatchButton() {
        urlSteps.open();
        basePageSteps.onOfferCardPage().egrnBlock().purchasedReports().should(hasSize(1)).get(0)
                .link(SEE_BUTTON).should(hasHref(containsString(EGRN_REPORT)));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Покупаем второй отчет -> появляется два отчета на карточке оффера")
    public void shouldSeeTwoReports() {
        urlSteps.open();
        basePageSteps.onOfferCardPage().egrnBlock().checkDataButton().click();
        clickBuy();
        urlSteps.open();
        basePageSteps.refreshUntil(() -> basePageSteps.onOfferCardPage().egrnBlock().purchasedReports(), hasSize(2),
                EGRN_TIMEOUT);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Покупаем отчет -> видим его на листинге отчетов")
    public void shouldSeeReportOnReportsPage() {
        urlSteps.testing().path(MANAGEMENT_NEW_EGRN_REPORTS).open();
        managementSteps.refreshUntil(() -> managementSteps.onEgrnListingPage().egrnSnippets(), hasSize(1), EGRN_TIMEOUT);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Покупаем отчет -> видим страницу отчета")
    public void shouldSeeReportPage() {
        urlSteps.testing().path(MANAGEMENT_NEW_EGRN_REPORTS).open();
        managementSteps.refreshUntil(() -> managementSteps.onEgrnListingPage().egrnSnippets(), hasSize(1), EGRN_TIMEOUT);
        managementSteps.onEgrnListingPage().firstEgrnSnippet().link().click();
        managementSteps.onEgrnReportPage().errorPage().should(not(isDisplayed()));
        managementSteps.onEgrnReportPage().h2Title("Общие сведения").should(not(isDisplayed()));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Покупаем отчет -> видим транзакцию")
    public void shouldSeeTransaction() {
        urlSteps.testing().path(WALLET).open();
        walletSteps.onWalletPage().transactionHistory().paymentList().waitUntil(hasSize(1)).get(0).type()
                .should(hasText("«Отчёт о квартире»"));
    }

    private void clickBuy() {
        basePageSteps.onOfferCardPage().egrnBlock().button(BUY_FULL_REPORT).click();
        walletSteps.onWalletPage().cardsPopup().paymentButton()
                .waitUntil("Ждали когда появится кнопка оплаты", isDisplayed(), 30).click();
        walletSteps.onWalletPage().cardsPopup().spinVisible().waitUntil(not(isDisplayed()));
        urlSteps.shouldUrl(containsString(EGRN_REPORT));
    }
}
