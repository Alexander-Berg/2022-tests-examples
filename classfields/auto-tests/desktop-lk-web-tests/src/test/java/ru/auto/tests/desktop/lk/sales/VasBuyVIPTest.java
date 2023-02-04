package ru.auto.tests.desktop.lk.sales;

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
import ru.auto.tests.desktop.categories.Billing;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.element.BillingPopup;
import ru.auto.tests.desktop.mock.beans.stub.Query;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.YaKassaSteps;
import ru.auto.tests.publicapi.model.AutoApiOffer;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Notifications.PACKAGE_VIP_ACTIVATED;
import static ru.auto.tests.desktop.consts.Owners.DENISKOROBOV;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.SaleServices.VasProduct.ALL_SALE_COLORS;
import static ru.auto.tests.desktop.consts.SaleServices.VasProduct.ALL_SALE_SPECIAL;
import static ru.auto.tests.desktop.consts.SaleServices.VasProduct.ALL_SALE_TOPLIST;
import static ru.auto.tests.desktop.consts.SaleServices.VasProduct.PACKAGE_VIP;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserOffer.USER_OFFER_CAR_EXAMPLE;
import static ru.auto.tests.desktop.mock.MockUserOffer.mockUserOffer;
import static ru.auto.tests.desktop.mock.MockUserOffer.service;
import static ru.auto.tests.desktop.mock.MockUserOffers.userOffersResponse;
import static ru.auto.tests.desktop.mock.MockUserOffersCount.offersCount;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_CARS;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_CARS_COUNT;
import static ru.auto.tests.desktop.step.CookieSteps.FORCE_DISABLE_TRUST;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Покупка услуги «VIP»")
@Feature(AutoruFeatures.LK)
@Feature(AutoruFeatures.MY_OFFERS_PRIVATE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class VasBuyVIPTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private YaKassaSteps yaKassaSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() {
        cookieSteps.setExpFlags(FORCE_DISABLE_TRUST);

        mockRule.setStubs(
                stub().withGetDeepEquals(USER_OFFERS_CARS).withResponseBody(
                        userOffersResponse().setOffers(
                                mockUserOffer(USER_OFFER_CAR_EXAMPLE).addHighPriorityForServicePrice(PACKAGE_VIP)
                        ).build()),
                stub().withGetDeepEquals(USER_OFFERS_CARS_COUNT).withRequestQuery(Query.query()
                                .setCategory(AutoApiOffer.CategoryEnum.CARS.getValue()))
                        .withResponseBody(offersCount().getBody()),
                stub("desktop/SessionAuthUser"),
                stub("desktop/User"),
                stub("desktop-lk/BillingAutoruPaymentInitVip"),
                stub("desktop-lk/BillingAutoruPaymentProcess"),
                stub("desktop-lk/BillingAutoruPayment")
        ).create();

        urlSteps.testing().path(MY).path(CARS).open();
    }

    @Test
    @Owner(DENISKOROBOV)
    @Category({Regression.class, Testing.class, Billing.class})
    @DisplayName("Покупка услуги «Пакет VIP»")
    public void shouldBuyPackageVip() {
        basePageSteps.onLkSalesPage().getSale(0).click();
        basePageSteps.onLkSalesPage().getSale(0).vas().buyButton().should(isDisplayed()).click();
        basePageSteps.onLkSalesPage().switchToBillingFrame();

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

        basePageSteps.onLkSalesPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.onLkSalesPage().billingPopup().header().waitUntil(hasText(BillingPopup.PACKAGE_VIP));
        basePageSteps.onLkSalesPage().billingPopup().priceHeader().waitUntil(hasText("3 975 \u20BD"));
        yaKassaSteps.payWithCard();
        yaKassaSteps.waitForSuccessMessage();
        basePageSteps.onLkSalesPage().notifier(PACKAGE_VIP_ACTIVATED).waitUntil(isDisplayed());
        basePageSteps.onLkSalesPage().billingPopupCloseButton().click();
        basePageSteps.onLkSalesPage().billingPopupFrame().waitUntil(not(isDisplayed()));

        basePageSteps.onLkSalesPage().getSale(0).activatedTopIcon().should(isDisplayed());
        basePageSteps.onLkSalesPage().getSale(0).activatedColorIcon().should(isDisplayed());
        basePageSteps.onLkSalesPage().getSale(0).vas().tabsList().should(hasSize(2));
        basePageSteps.onLkSalesPage().getSale(0).vas().buyButton().should(not(isDisplayed()));
    }

}
