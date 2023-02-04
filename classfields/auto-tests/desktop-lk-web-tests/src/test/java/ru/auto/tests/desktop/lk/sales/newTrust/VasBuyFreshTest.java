package ru.auto.tests.desktop.lk.sales.newTrust;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
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

import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.NEW_TRUST;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.SaleServices.VasProduct.ALL_SALE_FRESH;
import static ru.auto.tests.desktop.mock.MockBillingPaymentInitRequest.paymentInitVasRequest;
import static ru.auto.tests.desktop.mock.MockBillingPaymentInitResponse.paymentInitVasResponse;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserOffer.USER_OFFER_CAR_EXAMPLE;
import static ru.auto.tests.desktop.mock.MockUserOffer.USER_OFFER_MOTO_EXAMPLE;
import static ru.auto.tests.desktop.mock.MockUserOffer.USER_OFFER_TRUCK_EXAMPLE;
import static ru.auto.tests.desktop.mock.MockUserOffer.mockUserOffer;
import static ru.auto.tests.desktop.mock.MockUserOffer.service;
import static ru.auto.tests.desktop.mock.MockUserOffers.userOffersResponse;
import static ru.auto.tests.desktop.mock.MockUserOffersCount.offersCount;
import static ru.auto.tests.desktop.mock.Paths.BILLING_AUTORU_PAYMENT_INIT;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS;
import static ru.auto.tests.desktop.mock.beans.stub.Query.query;
import static ru.auto.tests.desktop.step.CookieSteps.FORCE_IGNORE_TRUST_EXP_RESULT;
import static ru.auto.tests.desktop.utils.Utils.getRandomBetween;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Покупка услуги «Поднятие в поиске»")
@Epic(AutoruFeatures.LK)
@Feature(AutoruFeatures.MY_OFFERS_PRIVATE)
@Story(NEW_TRUST)
@RunWith(Parameterized.class)
@GuiceModules(DesktopDevToolsTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class VasBuyFreshTest {

    private static final String ID = "1076842087-f1e84";
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

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String userOfferPathToMock;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"cars", USER_OFFER_CAR_EXAMPLE},
                {"trucks", USER_OFFER_TRUCK_EXAMPLE},
                {"moto", USER_OFFER_MOTO_EXAMPLE}
        });
    }

    @Before
    public void before() {
        seleniumMockSteps.setNewTrustBillingBrowserMock();

        cookieSteps.setExpFlags(FORCE_IGNORE_TRUST_EXP_RESULT);

        mockRule.setStubs(
                stub().withGetDeepEquals(format("%s/%s", USER_OFFERS, category))
                        .withResponseBody(
                                userOffersResponse().setOffers(
                                        mockUserOffer(userOfferPathToMock)
                                ).build()),

                stub().withGetDeepEquals(format("%s/%s/count", USER_OFFERS, category))
                        .withRequestQuery(
                                query().setCategory(category))
                        .withResponseBody(
                                offersCount().getBody()),

                stub().withPostDeepEquals(BILLING_AUTORU_PAYMENT_INIT)
                        .withRequestBody(
                                paymentInitVasRequest()
                                        .setProducts(ALL_SALE_FRESH).getBody())
                        .withResponseBody(
                                paymentInitVasResponse()
                                        .setCost(COST)
                                        .setDetailedProductInfos(ALL_SALE_FRESH)
                                        .getBody()),

                stub("desktop-lk/NewTrustStart"),
                stub("desktop-lk/NewTrustPayment"),
                stub("desktop/SessionAuthUser"),
                stub("desktop/User")
        ).create();

        urlSteps.testing().path(MY).path(category).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class, Billing.class})
    @DisplayName("Покупка услуги «Поднятие в поиске»")
    public void shouldBuyVas() {
        basePageSteps.onLkSalesPage().getSale(0).freshButton().should(isDisplayed()).click();

        mockRule.overwriteStub(0,
                stub().withGetDeepEquals(format("%s/%s/%s", USER_OFFERS, category, ID))
                        .withResponseBody(
                                mockUserOffer(userOfferPathToMock).setServices(
                                        service(ALL_SALE_FRESH)
                                ).getOffer()));

        newTrustSteps.payForVas(ALL_SALE_FRESH.getName(), COST, "Опция «Поднятие в поиске» подключена");

        basePageSteps.onLkSalesPage().getSale(0).freshButton().should(isDisplayed());
    }

}
