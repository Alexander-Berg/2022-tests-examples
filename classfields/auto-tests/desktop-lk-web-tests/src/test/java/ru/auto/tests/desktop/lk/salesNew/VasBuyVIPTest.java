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
import ru.auto.tests.desktop.step.YaKassaSteps;
import ru.auto.tests.publicapi.model.AutoApiOffer;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.SaleServices.VasProduct.PACKAGE_VIP;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserOffer.USER_OFFER_CAR_EXAMPLE;
import static ru.auto.tests.desktop.mock.MockUserOffer.mockUserOffer;
import static ru.auto.tests.desktop.mock.MockUserOffer.service;
import static ru.auto.tests.desktop.mock.MockUserOffers.userOffersResponse;
import static ru.auto.tests.desktop.mock.MockUserOffersCount.offersCount;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_CARS;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_CARS_COUNT;
import static ru.auto.tests.desktop.step.CookieSteps.EXP_AUTORUFRONT_19219;
import static ru.auto.tests.desktop.step.CookieSteps.FORCE_DISABLE_TRUST;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Покупка услуги «VIP»")
@Epic(AutoruFeatures.LK_NEW)
@Feature(AutoruFeatures.MY_OFFERS_PRIVATE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
@Ignore
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
        cookieSteps.setExpFlags(EXP_AUTORUFRONT_19219, FORCE_DISABLE_TRUST);

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
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class, Billing.class})
    @DisplayName("Покупка услуги «Пакет VIP»")
    public void shouldBuyPackageVip() {
        basePageSteps.onLkSalesNewPage().getSale(0).vasItem("VIP").click();
        basePageSteps.onLkSalesNewPage().switchToBillingFrame();

        mockRule.overwriteStub(0,
                stub().withGetDeepEquals(USER_OFFERS_CARS).withResponseBody(
                        userOffersResponse().setOffers(
                                mockUserOffer(USER_OFFER_CAR_EXAMPLE)
                                        .addHighPriorityForServicePrice(PACKAGE_VIP)
                                        .setServices(service(PACKAGE_VIP))
                        ).build()));

        basePageSteps.onLkSalesNewPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.onLkSalesNewPage().billingPopup().header().waitUntil(hasText("Пакет VIP"));
        basePageSteps.onLkSalesNewPage().billingPopup().priceHeader().waitUntil(hasText("3 975 \u20BD"));
        yaKassaSteps.payWithCard();
        yaKassaSteps.waitForSuccessMessage();

        basePageSteps.switchToDefaultFrame();
        basePageSteps.onLkSalesNewPage().billingPopupCloseButton().click();
        basePageSteps.onLkSalesNewPage().billingPopupFrame().waitUntil(not(isDisplayed()));

        basePageSteps.onLkSalesNewPage().getSale(0).getVas(0)
                .should(hasText(matchesPattern("×200 просмотров\nАктивно ещё\\d+ (день|дня|дней)")));
    }

}
