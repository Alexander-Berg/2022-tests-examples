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
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.config.RealtyWebConfig;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.UrlSteps;
import ru.yandex.realty.step.WalletSteps;
import ru.yandex.realty.utils.AccountType;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static ru.auto.test.api.realty.OfferType.APARTMENT_SELL;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.OfferByRegion.Region.EGRN_SUCCESS;
import static ru.yandex.realty.consts.OfferByRegion.getLocationForRegion;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.OFFER;
import static ru.yandex.realty.element.offercard.EgrnBlock.BUY_FULL_REPORT;
import static ru.yandex.realty.element.offercard.EgrnBlock.ENTER_BUTTON;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;
import static ru.yandex.realty.step.CommonSteps.EGRN_TIMEOUT;

@Issue("VERTISTEST-1509")
@DisplayName("Отчет ЕГРН. Карточка оффера")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class NotAuthOfferCardEgrnTest {

    private static final String AUTH = "auth";
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
    private WalletSteps walletSteps;

    @Inject
    private OfferBuildingSteps offerBuildingSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private CompareSteps compareSteps;

    @Inject
    private RealtyWebConfig config;

    @Before
    public void before() {
        apiSteps.createVos2AccountWithoutLogin(account, AccountType.OWNER);
        offerId = offerBuildingSteps.addNewOffer(account)
                .withBody(OfferBuildingSteps.getDefaultOffer(APARTMENT_SELL)
                        .withLocation(getLocationForRegion(EGRN_SUCCESS))).create().getId();
        apiSteps.createVos2AccountWithoutLogin(account2, AccountType.OWNER);
        urlSteps.testing().path(OFFER).path(offerId).open();
        basePageSteps.refreshUntil(() -> basePageSteps.onOfferCardPage().egrnBlock(), exists(), EGRN_TIMEOUT);
        basePageSteps.onOfferCardPage().egrnBlock().checkDataButton().click();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим «Войти» под незалогином")
    public void shouldSeeLoginButton() {
        basePageSteps.onOfferCardPage().egrnBlock().link(ENTER_BUTTON).waitUntil(isDisplayed());
        basePageSteps.onOfferCardPage().egrnBlock().link(ENTER_BUTTON)
                .should(hasHref(containsString(config.getPassportTestURL() + AUTH)));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим «Купить полный отчет за» под незалогином")
    public void shouldSeeBuyButton() {
        basePageSteps.onOfferCardPage().egrnBlock().button(BUY_FULL_REPORT).waitUntil(isDisplayed()).click();
        basePageSteps.refreshUntil(() -> basePageSteps.onPassportLoginPage().login(), isDisplayed());
        urlSteps.shouldUrl(allOf(containsString(config.getPassportTestURL().toString() + AUTH),
                containsString("fromEGRN%3Dpaid")));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("«Купить полный отчет за» -> авторизация -> форма оплаты")
    public void shouldSeeAuthAndBuyForm() {
        basePageSteps.refreshUntil(() -> {
                    urlSteps.testing().path(OFFER).path(offerId).open();
                    basePageSteps.onOfferCardPage().egrnBlock().checkDataButton().click();
                    basePageSteps.onOfferCardPage().egrnBlock().button(BUY_FULL_REPORT).click();
                    return basePageSteps.onPassportLoginPage().login();
                }, isDisplayed()
        );
        basePageSteps.onPassportLoginPage().loginInPassport(account2);
        walletSteps.onWalletPage().cardsPopup().should(isDisplayed());
    }
}
