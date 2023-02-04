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
import ru.auto.tests.desktop.consts.SaleServices;
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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.StringRegularExpression.matchesRegex;
import static ru.auto.tests.desktop.component.WithCheckbox.CHECKBOX_DISABLED;
import static ru.auto.tests.desktop.consts.AutoruFeatures.NEW_TRUST;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.SaleServices.VasProduct.ALL_SALE_TOPLIST;
import static ru.auto.tests.desktop.mock.MockBillingPaymentInitRequest.paymentInitVasRequest;
import static ru.auto.tests.desktop.mock.MockBillingPaymentInitResponse.getProductInfoTemplate;
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
import static ru.auto.tests.desktop.utils.Utils.formatPrice;
import static ru.auto.tests.desktop.utils.Utils.getRandomBetween;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Автопродление опции до/после оплаты")
@Epic(AutoruFeatures.LK)
@Feature(AutoruFeatures.MY_OFFERS_PRIVATE)
@Story(NEW_TRUST)
@RunWith(Parameterized.class)
@GuiceModules(DesktopDevToolsTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class VasAutoprolongOptionTest {

    private static final String ID = "1076842087-f1e84";
    private static final SaleServices.VasProduct VAS_PRODUCT = ALL_SALE_TOPLIST;
    private static final int DAYS_COUNT = getRandomBetween(3, 9);
    private static final int PROLONG_COST = getRandomBetween(100, 2000);
    private static final int COST = getRandomBetween(100, 2000);

    private static final String RECURRENT_BEFORE_CHECKOUT_TEMPLATE = "Включить автопродление опции «%s»\n%s каждые %s (дня|дней)";
    private static final String RECURRENT_AFTER_CHECKOUT_TEMPLATE = "Платёж совершён\n" +
            "Продайте быстрее и удобнее — включите автопродление опции «%s»\n" +
            "Включить автопродление\n%s каждые %s (дня|дней)";
    private static final String OPTION_ENABLED = "Опция «Поднятие в ТОП» подключена";


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
    private NewTrustSteps newTrustSteps;

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
                                        .setProducts(VAS_PRODUCT).getBody())
                        .withResponseBody(
                                paymentInitVasResponse()
                                        .tieCard()
                                        .setCost(COST)
                                        .setDetailedProductInfos(
                                                getProductInfoTemplate()
                                                        .setName(VAS_PRODUCT.getName())
                                                        .setService(VAS_PRODUCT.getValue())
                                                        .setProlongationAllowed(true)
                                                        .setProlongationForced(true)
                                                        .setProlongationForcedNotTogglable(false)
                                                        .setBasePrice(PROLONG_COST * 100)
                                                        .setDays(DAYS_COUNT))
                                        .getBody()),

                stub("desktop-lk/NewTrustStart"),
                stub("desktop-lk/NewTrustPayment"),
                stub("desktop-lk/NewTrustPaymentProcess"),
                stub("desktop/SessionAuthUser"),
                stub("desktop-lk/UserTransactionProlongable"),
                stub("desktop/User")
        ).create();

        urlSteps.testing().path(MY).path(category).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class, Billing.class})
    @DisplayName("Автопродление опции «Поднятие в ТОП» при оплате")
    public void shouldProlongOptionBeforeCheckout() {
        basePageSteps.onLkSalesPage().getSale(0).vas().getTab(2).should(isDisplayed()).click();
        basePageSteps.onLkSalesPage().getSale(0).vas().optionCheckbox(VAS_PRODUCT.getName()).click();
        basePageSteps.onLkSalesPage().getSale(0).vas().buyButton().should(isDisplayed()).click();

        mockRule.overwriteStub(0,
                stub().withGetDeepEquals(format("%s/%s/%s", USER_OFFERS, category, ID))
                        .withResponseBody(
                                mockUserOffer(userOfferPathToMock).setServices(
                                                service(VAS_PRODUCT).setProlongable(true))
                                        .getOffer()));

        basePageSteps.onLkSalesPage().switchToBillingFrame();

        basePageSteps.onLkSalesPage().billingPopup().waitUntil(isDisplayed());

        basePageSteps.onLkSalesPage().newTrust().recurrentBlock().should(
                hasText(matchesRegex(format(RECURRENT_BEFORE_CHECKOUT_TEMPLATE,
                        VAS_PRODUCT.getName(), formatPrice(PROLONG_COST), DAYS_COUNT))));

        basePageSteps.onLkSalesPage().newTrust().title().waitUntil(hasText(VAS_PRODUCT.getName()));
        basePageSteps.onLkSalesPage().newTrust().price().waitUntil(hasText(formatPrice(COST)));
        basePageSteps.onLkSalesPage().newTrust().recurrentBlock().enabledSwitcher().waitUntil(isDisplayed());

        newTrustSteps.payWithTiedCard();
        newTrustSteps.waitForSuccessMessage();

        basePageSteps.onLkSalesPage().notifier().waitUntil(isDisplayed())
                .waitUntil(hasText(OPTION_ENABLED));

        basePageSteps.onLkSalesPage().billingPopupCloseButton().waitUntil(isDisplayed()).click();
        basePageSteps.onLkSalesPage().billingPopupFrame().waitUntil(not(isDisplayed()));

        basePageSteps.onLkSalesPage().getSale(0).vas().option(VAS_PRODUCT.getName())
                .should(hasText(format("%s\nАвтопродление включено", VAS_PRODUCT.getName())));
        basePageSteps.onLkSalesPage().getSale(0).vas().optionCheckbox(VAS_PRODUCT.getName())
                .should(hasClass(containsString(CHECKBOX_DISABLED)));
        basePageSteps.onLkSalesPage().getSale(0).activatedTopIcon().waitUntil(isDisplayed());
        basePageSteps.onLkSalesPage().getSale(0).vas().tabsList().should(hasSize(3));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class, Billing.class})
    @DisplayName("Автопродление опции «Поднятие в ТОП» после оплаты")
    public void shouldProlongOptionAfterCheckout() {
        basePageSteps.onLkSalesPage().getSale(0).vas().getTab(2).should(isDisplayed()).click();
        basePageSteps.onLkSalesPage().getSale(0).vas().optionCheckbox(VAS_PRODUCT.getName()).click();
        basePageSteps.onLkSalesPage().getSale(0).vas().buyButton().should(isDisplayed()).click();

        mockRule.overwriteStub(0,
                stub().withGetDeepEquals(format("%s/%s/%s", USER_OFFERS, category, ID))
                        .withResponseBody(
                                mockUserOffer(userOfferPathToMock).setServices(
                                                service(VAS_PRODUCT).setProlongable(true))
                                        .getOffer()));

        basePageSteps.onLkSalesPage().switchToBillingFrame();

        basePageSteps.onLkSalesPage().billingPopup().waitUntil(isDisplayed());

        basePageSteps.onLkSalesPage().newTrust().recurrentBlock().should(
                hasText(matchesRegex(format("Включить автопродление опции «%s»\n%s каждые %s (дня|дней)",
                        VAS_PRODUCT.getName(), formatPrice(PROLONG_COST), DAYS_COUNT))));

        basePageSteps.onLkSalesPage().newTrust().title().waitUntil(hasText(VAS_PRODUCT.getName()));
        basePageSteps.onLkSalesPage().newTrust().price().waitUntil(hasText(formatPrice(COST)));
        basePageSteps.onLkSalesPage().newTrust().recurrentBlock().enabledSwitcher().waitUntil(isDisplayed()).click();
        basePageSteps.onLkSalesPage().newTrust().recurrentBlock().disabledSwitcher().waitUntil(isDisplayed());

        newTrustSteps.payWithTiedCard();
        basePageSteps.switchToDefaultFrame();

        basePageSteps.onLkSalesPage().notifier().waitUntil(isDisplayed())
                .waitUntil(hasText(OPTION_ENABLED));
        basePageSteps.onLkSalesPage().switchToBillingFrame();

        mockRule.setStubs(
                stub().withPutDeepEquals(
                                format("%s/%s/%s/product/all_sale_toplist/prolongable", USER_OFFERS, category, ID))
                        .withStatusSuccessResponse()
        ).update();

        basePageSteps.onBasePage().newTrust().waitUntil(hasText(matchesRegex(format(RECURRENT_AFTER_CHECKOUT_TEMPLATE,
                VAS_PRODUCT.getName(), formatPrice(PROLONG_COST), DAYS_COUNT))));
        basePageSteps.onBasePage().newTrust().buttonContains("Включить автопродление").click();
        basePageSteps.switchToDefaultFrame();

        basePageSteps.onLkSalesPage().notifier().waitUntil(isDisplayed())
                .waitUntil(hasText("Автопродление включено"));
        basePageSteps.onLkSalesPage().billingPopupFrame().waitUntil(not(isDisplayed()));

        basePageSteps.onLkSalesPage().getSale(0).vas().option(VAS_PRODUCT.getName())
                .should(hasText(format("%s\nАвтопродление включено", VAS_PRODUCT.getName())));
        basePageSteps.onLkSalesPage().getSale(0).vas().optionCheckbox(VAS_PRODUCT.getName())
                .should(hasClass(containsString(CHECKBOX_DISABLED)));
        basePageSteps.onLkSalesPage().getSale(0).activatedTopIcon().waitUntil(isDisplayed());
        basePageSteps.onLkSalesPage().getSale(0).vas().tabsList().should(hasSize(3));
    }

}
