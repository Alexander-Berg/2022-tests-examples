package ru.yandex.realty.egrn.offercard;

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
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.mobile.step.PaymentSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.UrlSteps;
import ru.yandex.realty.utils.AccountType;

import static org.hamcrest.CoreMatchers.containsString;
import static ru.auto.test.api.realty.OfferType.APARTMENT_SELL;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.OfferByRegion.Region.EGRN_SUCCESS;
import static ru.yandex.realty.consts.OfferByRegion.getLocationForRegion;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.OFFER;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;
import static ru.yandex.realty.mobile.element.offercard.EgrnBlock.BUY_REPORT;
import static ru.yandex.realty.mobile.element.offercard.EgrnBlock.SEE_FREE_REPORT;
import static ru.yandex.realty.step.CommonSteps.EGRN_TIMEOUT;

@Issue("VERTISTEST-1509")
@DisplayName("Отчет ЕГРН. Карточка оффера")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class NotAuthOfferCardEgrnTest {

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
    private PaymentSteps paymentSteps;

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
        String offerId = offerBuildingSteps.addNewOffer(account)
                .withBody(OfferBuildingSteps.getDefaultOffer(APARTMENT_SELL)
                        .withLocation(getLocationForRegion(EGRN_SUCCESS))).create().getId();
        apiSteps.createVos2AccountWithoutLogin(account2, AccountType.OWNER);
        basePageSteps.resize(390, 3000);
        urlSteps.testing().path(OFFER).path(offerId).open();
        basePageSteps.refreshUntil(() -> basePageSteps.onOfferCardPage().egrnBlock(), exists(), EGRN_TIMEOUT);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим «Смотреть бесплатный отчёт» под незалогином")
    public void shouldSeeLoginButton() {
        basePageSteps.onOfferCardPage().egrnBlock().link(SEE_FREE_REPORT).waitUntil(isDisplayed());
        basePageSteps.onOfferCardPage().egrnBlock().link(SEE_FREE_REPORT)
                .should(hasHref(containsString(config.getPassportTestURL() + "auth")));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим «Купить полный отчет за» под незалогином")
    public void shouldSeeBuyButton() {
        basePageSteps.onOfferCardPage().egrnBlock().link(BUY_REPORT).waitUntil(isDisplayed());
        basePageSteps.onOfferCardPage().egrnBlock().link(BUY_REPORT)
                .should(hasHref(containsString(config.getPassportTestURL() + "auth")));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("«Купить полный отчет за» -> авторизация -> форма оплаты")
    public void shouldSeeAuthAndBuyForm() {
        basePageSteps.scrollToElement(basePageSteps.onOfferCardPage().egrnBlock().link(BUY_REPORT));
        basePageSteps.onOfferCardPage().egrnBlock().link(BUY_REPORT).click();
        basePageSteps.onPassportLoginPage().loginInPassport(account2);
        paymentSteps.onPaymentPopupPage().paymentVisible().should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот блока ЕГРН под незалогином")
    public void shouldSeeOfferNotAuthEgrnBlock() {
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onOfferCardPage().egrnBlock());
        urlSteps.setMobileProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onOfferCardPage().egrnBlock());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}
