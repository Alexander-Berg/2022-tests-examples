package ru.auto.tests.desktop.lk.salesNew;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Billing;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mock.beans.stub.Query;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.publicapi.model.AutoApiOffer;

import static java.lang.String.format;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.SaleServices.VasProduct.ALL_SALE_COLORS;
import static ru.auto.tests.desktop.consts.SaleServices.VasProduct.ALL_SALE_SPECIAL;
import static ru.auto.tests.desktop.consts.SaleServices.VasProduct.ALL_SALE_TOPLIST;
import static ru.auto.tests.desktop.consts.SaleServices.VasProduct.PACKAGE_TURBO;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserOffer.USER_OFFER_CAR_EXAMPLE;
import static ru.auto.tests.desktop.mock.MockUserOffer.mockUserOffer;
import static ru.auto.tests.desktop.mock.MockUserOffer.service;
import static ru.auto.tests.desktop.mock.MockUserOffers.userOffersResponse;
import static ru.auto.tests.desktop.mock.MockUserOffersCount.offersCount;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_CARS;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_CARS_COUNT;
import static ru.auto.tests.desktop.mock.beans.stub.Query.query;
import static ru.auto.tests.desktop.step.CookieSteps.EXP_AUTORUFRONT_19219;
import static ru.auto.tests.desktop.step.CookieSteps.FORCE_DISABLE_TRUST;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Оплата привязанной картой")
@Epic(AutoruFeatures.LK_NEW)
@Feature(AutoruFeatures.MY_OFFERS_PRIVATE)
@RunWith(GuiceTestRunner.class)
@Ignore
@GuiceModules(DesktopTestsModule.class)
public class VasTiedCardTest {

    private static final String ID_PACKAGE_TURBO = "/1076842087-f1e84/product/package_turbo/prolongable";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() {
        cookieSteps.setExpFlags(EXP_AUTORUFRONT_19219, FORCE_DISABLE_TRUST);

        mockRule.setStubs(
                stub().withGetDeepEquals(USER_OFFERS_CARS).withResponseBody(
                        userOffersResponse().setOffers(
                                mockUserOffer(USER_OFFER_CAR_EXAMPLE)
                        ).build()),
                stub().withPutDeepEquals(format("%s%s", USER_OFFERS_CARS, ID_PACKAGE_TURBO))
                        .withRequestQuery(query().setDomain("autoru"))
                        .withStatusSuccessResponse(),
                stub().withGetDeepEquals(USER_OFFERS_CARS_COUNT).withRequestQuery(Query.query()
                                .setCategory(AutoApiOffer.CategoryEnum.CARS.getValue()))
                        .withResponseBody(offersCount().getBody()),
                stub("desktop/SessionAuthUser"),
                stub("desktop-lk/BillingAutoruPaymentInitTurboTiedCard"),
                stub("desktop-lk/BillingAutoruPaymentProcessTiedCardModalNotifier"),
                stub("desktop-lk/BillingAutoruPayment"),
                stub("desktop-lk/UserTransactionProlongable")
        ).create();

        urlSteps.testing().path(MY).path(CARS).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class, Billing.class})
    @DisplayName("Покупка услуги «Турбо-продажа», оплата привязанной картой")
    public void shouldBuyTurboPackageTiedCard() {
        basePageSteps.onLkSalesNewPage().getSale(0).vasItem("Турбо-продажа").click();
        basePageSteps.onLkSalesNewPage().switchToBillingFrame();

        mockRule.overwriteStub(0,
                stub().withGetDeepEquals(USER_OFFERS_CARS).withResponseBody(
                        userOffersResponse().setOffers(
                                mockUserOffer(USER_OFFER_CAR_EXAMPLE)
                                        .addProlongationAllowedForServicePrice(PACKAGE_TURBO)
                                        .setServices(
                                                service(PACKAGE_TURBO).setProlongable(true),
                                                service(ALL_SALE_COLORS),
                                                service(ALL_SALE_SPECIAL),
                                                service(ALL_SALE_TOPLIST))
                        ).build()));

        basePageSteps.onLkSalesNewPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.onLkSalesNewPage().billingPopup().header().waitUntil(hasText("Турбо продажа"));
        basePageSteps.onLkSalesNewPage().billingPopup().priceHeader().waitUntil(hasText("897 \u20BD"));
        basePageSteps.onLkSalesNewPage().billingPopup().tiedCardPayButton().waitUntil(isDisplayed()).click();
        basePageSteps.onLkSalesNewPage().billingPopup().successMessage().waitUntil(hasText("Платёж совершён успешно"));

        basePageSteps.switchToDefaultFrame();
        basePageSteps.onLkSalesNewPage().billingPopupCloseButton().click();
        basePageSteps.onLkSalesNewPage().billingPopupFrame().waitUntil(not(isDisplayed()));

        basePageSteps.onLkSalesNewPage().getSale(0).getVas(0).should(hasText("×20 просмотров\nАвтопродление"));
    }
}
