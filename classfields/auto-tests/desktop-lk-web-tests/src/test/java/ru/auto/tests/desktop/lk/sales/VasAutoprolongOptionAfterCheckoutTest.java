package ru.auto.tests.desktop.lk.sales;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
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

import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Owners.DENISKOROBOV;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.consts.SaleServices.VasProduct.ALL_SALE_TOPLIST;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserOffer.USER_OFFER_CAR_EXAMPLE;
import static ru.auto.tests.desktop.mock.MockUserOffer.USER_OFFER_MOTO_EXAMPLE;
import static ru.auto.tests.desktop.mock.MockUserOffer.USER_OFFER_TRUCK_EXAMPLE;
import static ru.auto.tests.desktop.mock.MockUserOffer.mockUserOffer;
import static ru.auto.tests.desktop.mock.MockUserOffer.service;
import static ru.auto.tests.desktop.mock.MockUserOffers.userOffersResponse;
import static ru.auto.tests.desktop.mock.MockUserOffersCount.offersCount;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_CARS;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_CARS_COUNT;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_MOTO;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_MOTO_COUNT;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_TRUCKS;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_TRUCKS_COUNT;
import static ru.auto.tests.desktop.mock.beans.stub.Query.query;
import static ru.auto.tests.desktop.step.CookieSteps.FORCE_DISABLE_TRUST;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Автопродление опции после оплаты")
@Epic(AutoruFeatures.LK)
@Feature(AutoruFeatures.MY_OFFERS_PRIVATE)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class VasAutoprolongOptionAfterCheckoutTest {

    private static final String ID = "1076842087-f1e84";

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

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String userOfferPath;

    @Parameterized.Parameter(2)
    public String userOfferPathToMock;

    @Parameterized.Parameter(3)
    public String userOfferCountPath;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, USER_OFFERS_CARS, USER_OFFER_CAR_EXAMPLE, USER_OFFERS_CARS_COUNT},
                {TRUCKS, USER_OFFERS_TRUCKS, USER_OFFER_TRUCK_EXAMPLE, USER_OFFERS_TRUCKS_COUNT},
                {MOTO, USER_OFFERS_MOTO, USER_OFFER_MOTO_EXAMPLE, USER_OFFERS_MOTO_COUNT}
        });
    }

    @Before
    public void before() {
        cookieSteps.setExpFlags(FORCE_DISABLE_TRUST);

        mockRule.setStubs(
                stub().withGetDeepEquals(userOfferPath).withResponseBody(
                        userOffersResponse().setOffers(
                                mockUserOffer(userOfferPathToMock)
                        ).build()),
                stub().withPutDeepEquals(format("%s/%s/product/all_sale_toplist/prolongable", userOfferPath, ID))
                        .withRequestQuery(query().setDomain("autoru"))
                        .withStatusSuccessResponse(),
                stub().withGetDeepEquals(userOfferCountPath)
                        .withRequestQuery(Query.query().setCategory(category.replaceAll("/", "")))
                        .withResponseBody(offersCount().getBody()),
                stub("desktop/SessionAuthUser"),
                stub("desktop/User"),
                stub("desktop-lk/BillingAutoruPaymentInitToplistTiedCard"),
                stub("desktop-lk/BillingAutoruPaymentProcessTiedCardModalTopList"),
                stub("desktop-lk/BillingAutoruPaymentProcessTiedCardModalTrucksTopList"),
                stub("desktop-lk/BillingAutoruPayment"),
                stub("desktop-lk/UserTransactionProlongable")
        ).create();

        urlSteps.testing().path(MY).path(category).open();
    }

    @Test
    @Owner(DENISKOROBOV)
    @Category({Regression.class, Testing.class, Billing.class})
    @DisplayName("Автопродление опции «Поднятие в ТОП» после оплаты")
    public void shouldProlongOptionAfterCheckout() {
        basePageSteps.onLkSalesPage().getSale(0).vas().getTab(2).should(isDisplayed()).click();
        basePageSteps.onLkSalesPage().getSale(0).vas().optionCheckbox("Поднятие в ТОП").click();
        basePageSteps.onLkSalesPage().getSale(0).vas().buyButton().should(isDisplayed()).click();
        basePageSteps.onLkSalesPage().switchToBillingFrame();

        mockRule.overwriteStub(0,
                stub().withGetDeepEquals(format("%s/%s", userOfferPath, ID))
                        .withResponseBody(
                                mockUserOffer(userOfferPathToMock).setServices(service(ALL_SALE_TOPLIST)).getOffer()));

        basePageSteps.onLkSalesPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.onLkSalesPage().billingPopup().header().waitUntil(hasText("Поднятие в ТОП"));
        basePageSteps.onLkSalesPage().billingPopup().priceHeader().waitUntil(hasText("697 \u20BD"));
        basePageSteps.onLkSalesPage().billingPopup().autoprolongCheckbox().waitUntil(isDisplayed()).click();
        basePageSteps.onLkSalesPage().billingPopup().tiedCardPayButton().waitUntil(isDisplayed()).click();
        basePageSteps.onLkSalesPage().billingPopup().successMessage().waitUntil(hasText("Платёж совершён успешно"));
        basePageSteps.onLkSalesPage().billingPopup().autoProlongButton().waitUntil(isDisplayed()).click();
        basePageSteps.switchToDefaultFrame();
        basePageSteps.onLkSalesPage().notifier().waitUntil(isDisplayed()).waitUntil(hasText("Автопродление включено"));
        basePageSteps.onLkSalesPage().billingPopupFrame().waitUntil(not(isDisplayed()));
        basePageSteps.onLkSalesPage().getSale(0).vas().option("Поднятие в ТОП")
                .should(hasText("Поднятие в ТОП\nАвтопродление включено"));
        basePageSteps.onLkSalesPage().getSale(0).vas().optionCheckbox("Поднятие в ТОП")
                .should(hasClass(containsString("Checkbox_disabled")));
        basePageSteps.onLkSalesPage().getSale(0).activatedTopIcon().waitUntil(isDisplayed());
        basePageSteps.onLkSalesPage().getSale(0).vas().tabsList().should(hasSize(3));
    }

}
