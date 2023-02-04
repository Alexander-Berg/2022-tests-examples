package ru.auto.tests.desktop.lk.sales;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
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
import ru.auto.tests.desktop.mock.beans.stub.Query;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.publicapi.model.AutoApiOffer;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Notifications.PACKAGE_TURBO_ACTIVATED;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.SaleServices.VasProduct.ALL_SALE_COLORS;
import static ru.auto.tests.desktop.consts.SaleServices.VasProduct.ALL_SALE_SPECIAL;
import static ru.auto.tests.desktop.consts.SaleServices.VasProduct.ALL_SALE_TOPLIST;
import static ru.auto.tests.desktop.consts.SaleServices.VasProduct.PACKAGE_TURBO;
import static ru.auto.tests.desktop.element.BillingPopup.BANK_CARD;
import static ru.auto.tests.desktop.element.BillingPopup.PAYMENT_SUCCESS;
import static ru.auto.tests.desktop.element.BillingPopup.TURBO_SALE;
import static ru.auto.tests.desktop.mock.MockStub.sessionAuthUserStub;
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

@DisplayName("Оплата привязанной картой")
@Epic(AutoruFeatures.LK)
@Feature(AutoruFeatures.MY_OFFERS_PRIVATE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class VasTiedCardTest {

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
        cookieSteps.setExpFlags(FORCE_DISABLE_TRUST);

        mockRule.setStubs(
                sessionAuthUserStub(),
                stub("desktop-lk/BillingAutoruPaymentInitTurboTiedCard"),
                stub("desktop-lk/BillingAutoruPaymentProcessTiedCardModalNotifier"),
                stub("desktop-lk/BillingAutoruPayment"),
                stub().withGetDeepEquals(USER_OFFERS_CARS).withResponseBody(
                        userOffersResponse().setOffers(
                                mockUserOffer(USER_OFFER_CAR_EXAMPLE)).build()),
                stub().withGetDeepEquals(USER_OFFERS_CARS_COUNT).withRequestQuery(Query.query()
                                .setCategory(AutoApiOffer.CategoryEnum.CARS.getValue()))
                        .withResponseBody(offersCount().getBody())
        ).create();

        urlSteps.testing().path(MY).path(CARS).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class, Billing.class})
    @DisplayName("Оплата услуги «Турбо-продажа» привязанной картой")
    public void shouldBuyVas() {
        basePageSteps.onLkSalesPage().getSale(0).vas().buyButton().should(isDisplayed()).click();

        mockRule.overwriteStub(4,
                stub().withGetDeepEquals(USER_OFFERS_CARS).withResponseBody(
                        userOffersResponse().setOffers(
                                mockUserOffer(USER_OFFER_CAR_EXAMPLE)
                                        .setServices(
                                                service(PACKAGE_TURBO),
                                                service(ALL_SALE_COLORS),
                                                service(ALL_SALE_SPECIAL),
                                                service(ALL_SALE_TOPLIST))
                        ).build()));

        basePageSteps.onLkSalesPage().switchToBillingFrame();
        basePageSteps.onLkSalesPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.onLkSalesPage().billingPopup().header().waitUntil(hasText(TURBO_SALE));
        basePageSteps.onLkSalesPage().billingPopup().priceHeader().waitUntil(hasText("897 \u20BD"));
        basePageSteps.onLkSalesPage().billingPopup().getPaymentMethod(0).title().waitUntil(hasText(BANK_CARD));
        basePageSteps.onLkSalesPage().billingPopup().tiedCardPayButton().waitUntil(isDisplayed()).click();
        basePageSteps.onLkSalesPage().billingPopup().successMessage().waitUntil(hasText(PAYMENT_SUCCESS));
        basePageSteps.switchToDefaultFrame();
        basePageSteps.onLkSalesPage().notifier(PACKAGE_TURBO_ACTIVATED).waitUntil(isDisplayed());
        basePageSteps.onLkSalesPage().billingPopupCloseButton().click();
        basePageSteps.onLkSalesPage().billingPopupFrame().waitUntil(not(isDisplayed()));

        basePageSteps.onLkSalesPage().getSale(0).activatedTopIcon().should(isDisplayed());
        basePageSteps.onLkSalesPage().getSale(0).activatedColorIcon().should(isDisplayed());
        basePageSteps.onLkSalesPage().getSale(0).vas().tabsList().should(hasSize(3));
        basePageSteps.onLkSalesPage().getSale(0).vas().buyButton().should(not(isDisplayed()));
    }

}
