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
import ru.auto.tests.desktop.step.YaKassaSteps;

import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.consts.SaleServices.VasProduct.ALL_SALE_SPECIAL;
import static ru.auto.tests.desktop.consts.SaleServices.VasProduct.ALL_SALE_TOPLIST;
import static ru.auto.tests.desktop.consts.SaleServices.VasProduct.SHOW_IN_STORIES;
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
import static ru.auto.tests.desktop.step.CookieSteps.FORCE_DISABLE_TRUST;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Покупка наборного пакета")
@Epic(AutoruFeatures.LK)
@Feature(AutoruFeatures.MY_OFFERS_PRIVATE)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class VasBuyOptionsSetTest {

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
    private YaKassaSteps yaKassaSteps;

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
                stub().withGetDeepEquals(userOfferCountPath)
                        .withRequestQuery(Query.query().setCategory(category.replaceAll("/", "")))
                        .withResponseBody(offersCount().getBody()),

                stub("desktop/SessionAuthUser"),
                stub("desktop/User"),
                stub("desktop-lk/BillingAutoruPaymentInitToplistSpecial"),
                stub("desktop-lk/BillingAutoruPaymentProcess"),
                stub("desktop-lk/BillingAutoruPayment")
        ).create();

        urlSteps.testing().path(MY).path(category).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class, Billing.class})
    @DisplayName("Покупка наборного пакета")
    public void shouldBuyVas() {
        mockRule.overwriteStub(0,
                stub().withGetDeepEquals(format("%s/%s", userOfferPath, ID))
                        .withResponseBody(
                                mockUserOffer(userOfferPathToMock)
                                        .setServices(
                                                service(SHOW_IN_STORIES),
                                                service(ALL_SALE_TOPLIST),
                                                service(ALL_SALE_SPECIAL)
                                        ).getOffer()));

        basePageSteps.onLkSalesPage().getSale(0).vas().getTab(2).should(isDisplayed()).click();
        basePageSteps.onLkSalesPage().getSale(0).vas().optionCheckbox("Показ в Историях").click();
        basePageSteps.onLkSalesPage().getSale(0).vas().optionCheckbox("Поднятие в ТОП").click();
        basePageSteps.onLkSalesPage().getSale(0).vas().optionCheckbox("Спецпредложение").click();
        basePageSteps.onLkSalesPage().getSale(0).vas().buyButton().should(isDisplayed()).click();
        basePageSteps.onLkSalesPage().switchToBillingFrame();
        basePageSteps.onLkSalesPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.onLkSalesPage().billingPopup().header().waitUntil(hasText("Наборный пакет"));
        basePageSteps.onLkSalesPage().billingPopup().priceHeader().waitUntil(hasText("1 194 \u20BD"));
        yaKassaSteps.payWithCard();
        yaKassaSteps.waitForSuccessMessage();
        basePageSteps.onLkSalesPage().notifier().waitUntil(isDisplayed())
                .waitUntil(hasText("«Показ в Историях», «Поднятие в ТОП» и «Спецпредложение» подключены"));
        basePageSteps.onLkSalesPage().billingPopupCloseButton().click();
        basePageSteps.onLkSalesPage().billingPopupFrame().waitUntil(not(isDisplayed()));
        basePageSteps.onLkSalesPage().getSale(0).vas().tabsList().should(hasSize(3));
        basePageSteps.onLkSalesPage().getSale(0).activatedTopIcon().waitUntil(isDisplayed());
        basePageSteps.onLkSalesPage().getSale(0).vas().optionCheckbox("Показ в Историях")
                .should(hasClass(containsString("Checkbox_disabled")));
        basePageSteps.onLkSalesPage().getSale(0).vas().optionCheckbox("Поднятие в ТОП")
                .should(hasClass(containsString("Checkbox_disabled")));
        basePageSteps.onLkSalesPage().getSale(0).vas().optionCheckbox("Спецпредложение")
                .should(hasClass(containsString("Checkbox_disabled")));
        basePageSteps.onLkSalesPage().getSale(0).vas().option("Поднятие в ТОП").click();
        basePageSteps.onLkSalesPage().billingPopupFrame().should(not(isDisplayed()));
    }
}
