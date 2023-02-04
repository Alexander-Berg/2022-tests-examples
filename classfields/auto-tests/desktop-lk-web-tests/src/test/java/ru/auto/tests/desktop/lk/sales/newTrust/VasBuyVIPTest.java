package ru.auto.tests.desktop.lk.sales.newTrust;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Billing;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopDevToolsTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.NewTrustSteps;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.publicapi.model.AutoApiOffer;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.NEW_TRUST;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.SaleServices.VasProduct.ALL_SALE_COLORS;
import static ru.auto.tests.desktop.consts.SaleServices.VasProduct.ALL_SALE_SPECIAL;
import static ru.auto.tests.desktop.consts.SaleServices.VasProduct.ALL_SALE_TOPLIST;
import static ru.auto.tests.desktop.consts.SaleServices.VasProduct.PACKAGE_VIP;
import static ru.auto.tests.desktop.mock.MockBillingPaymentInitRequest.paymentInitVasRequest;
import static ru.auto.tests.desktop.mock.MockBillingPaymentInitResponse.paymentInitVasResponse;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserOffer.USER_OFFER_CAR_EXAMPLE;
import static ru.auto.tests.desktop.mock.MockUserOffer.mockUserOffer;
import static ru.auto.tests.desktop.mock.MockUserOffer.service;
import static ru.auto.tests.desktop.mock.MockUserOffers.userOffersResponse;
import static ru.auto.tests.desktop.mock.MockUserOffersCount.offersCount;
import static ru.auto.tests.desktop.mock.Paths.BILLING_AUTORU_PAYMENT_INIT;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_CARS;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_CARS_COUNT;
import static ru.auto.tests.desktop.mock.beans.stub.Query.query;
import static ru.auto.tests.desktop.step.CookieSteps.FORCE_IGNORE_TRUST_EXP_RESULT;
import static ru.auto.tests.desktop.utils.Utils.getRandomBetween;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Покупка услуги «VIP»")
@Epic(AutoruFeatures.LK)
@Feature(AutoruFeatures.MY_OFFERS_PRIVATE)
@Story(NEW_TRUST)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopDevToolsTestsModule.class)
public class VasBuyVIPTest {

    private static final int COST = getRandomBetween(100, 2000);

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private NewTrustSteps newTrustSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Inject
    private SeleniumMockSteps seleniumMockSteps;

    @Before
    public void before() {
        seleniumMockSteps.setNewTrustBillingBrowserMock();

        cookieSteps.setExpFlags(FORCE_IGNORE_TRUST_EXP_RESULT);

        mockRule.setStubs(
                stub().withGetDeepEquals(USER_OFFERS_CARS)
                        .withResponseBody(
                                userOffersResponse().setOffers(
                                        mockUserOffer(USER_OFFER_CAR_EXAMPLE).addHighPriorityForServicePrice(PACKAGE_VIP)
                                ).build()),

                stub().withGetDeepEquals(USER_OFFERS_CARS_COUNT)
                        .withRequestQuery(query()
                                .setCategory(AutoApiOffer.CategoryEnum.CARS.getValue()))
                        .withResponseBody(
                                offersCount().getBody()),

                stub().withPostDeepEquals(BILLING_AUTORU_PAYMENT_INIT)
                        .withRequestBody(
                                paymentInitVasRequest()
                                        .setProducts(PACKAGE_VIP).getBody())
                        .withResponseBody(
                                paymentInitVasResponse()
                                        .setCost(COST)
                                        .setDetailedProductInfos(PACKAGE_VIP)
                                        .getBody()),

                stub("desktop-lk/NewTrustStart"),
                stub("desktop-lk/NewTrustPayment"),
                stub("desktop/SessionAuthUser"),
                stub("desktop/User")
        ).create();

        urlSteps.testing().path(MY).path(CARS).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class, Billing.class})
    @DisplayName("Покупка услуги «Пакет VIP»")
    public void shouldBuyPackageVip() {
        basePageSteps.onLkSalesPage().getSale(0).click();
        basePageSteps.onLkSalesPage().getSale(0).vas().buyButton().should(isDisplayed()).click();

        mockRule.overwriteStub(0,
                stub().withGetDeepEquals(USER_OFFERS_CARS).withResponseBody(
                        userOffersResponse().setOffers(
                                mockUserOffer(USER_OFFER_CAR_EXAMPLE)
                                        .addHighPriorityForServicePrice(PACKAGE_VIP)
                                        .setServices(
                                                service(PACKAGE_VIP),
                                                service(ALL_SALE_COLORS),
                                                service(ALL_SALE_SPECIAL),
                                                service(ALL_SALE_TOPLIST))
                        ).build()));

        newTrustSteps.payForVas(PACKAGE_VIP.getName(), COST, "Пакет «VIP» подключён");

        basePageSteps.onLkSalesPage().getSale(0).activatedTopIcon().should(isDisplayed());
        basePageSteps.onLkSalesPage().getSale(0).activatedColorIcon().should(isDisplayed());
        basePageSteps.onLkSalesPage().getSale(0).vas().tabsList().should(hasSize(2));
        basePageSteps.onLkSalesPage().getSale(0).vas().buyButton().should(not(isDisplayed()));
    }

}
