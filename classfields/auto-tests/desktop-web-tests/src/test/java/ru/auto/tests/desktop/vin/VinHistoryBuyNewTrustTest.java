package ru.auto.tests.desktop.vin;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
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
import ru.auto.tests.desktop.module.DesktopDevToolsTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.NewTrustSteps;
import ru.auto.tests.desktop.step.PublicApiSteps;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.commons.mountebank.http.predicates.PredicateType.DEEP_EQUALS;
import static ru.auto.tests.desktop.consts.AutoruFeatures.NEW_TRUST;
import static ru.auto.tests.desktop.consts.AutoruFeatures.VIN;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.HISTORY;
import static ru.auto.tests.desktop.element.history.VinReportPreview.BUY_PACKAGE;
import static ru.auto.tests.desktop.element.history.VinReportPreview.ONE_REPORT;
import static ru.auto.tests.desktop.mock.MockBillingPaymentInitRequest.paymentInitHistoryRequest;
import static ru.auto.tests.desktop.mock.MockBillingPaymentInitResponse.getHistoryReportProductInfo;
import static ru.auto.tests.desktop.mock.MockBillingPaymentInitResponse.paymentInitVasResponse;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.BILLING_AUTORU_PAYMENT_INIT;
import static ru.auto.tests.desktop.mock.beans.stub.Query.query;
import static ru.auto.tests.desktop.step.CookieSteps.FORCE_IGNORE_TRUST_EXP_RESULT;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(VIN)
@Story(NEW_TRUST)
@DisplayName("Страница истории автомобиля")
@GuiceModules(DesktopDevToolsTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class VinHistoryBuyNewTrustTest {

    private static final String REPORT_TITLE = "Отчёт о проверке по VIN";
    private static final String REPORT_DESCRIPTION = "Пакет из 10 отчётов ПроАвто. Действует 1 год";
    private static final String VIN = "4S2CK58D924333406";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private NewTrustSteps newTrustSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Inject
    private PublicApiSteps publicApiSteps;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private SeleniumMockSteps seleniumMockSteps;

    @Before
    public void before() {
        cookieSteps.setExpFlags(FORCE_IGNORE_TRUST_EXP_RESULT);

        seleniumMockSteps.setNewTrustBillingBrowserMock();

        mockRule.setStubs(
                stub("desktop/CarfaxReportRawVinNotPaid"),
                stub("desktop/SessionAuthUser"),
                stub("desktop/User"),
                stub("desktop-lk/NewTrustStart"),
                stub("desktop-lk/NewTrustPayment"),
                stub("desktop-lk/NewTrustPaymentProcess"),
                stub("desktop/BillingSubscriptionsOffersHistoryReportsPricesWithDescription"),

                stub().withPostDeepEquals(BILLING_AUTORU_PAYMENT_INIT)
                        .withRequestBody(
                                paymentInitHistoryRequest()
                                        .setSubscribePurchaseCount(1).getBody())
                        .withResponseBody(
                                paymentInitVasResponse()
                                        .setDetailedProductInfos(
                                                getHistoryReportProductInfo()
                                        ).getBody())
        ).create();

        urlSteps.testing().path(HISTORY).path(VIN).open();

    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class, Billing.class})
    @DisplayName("Покупка одного отчета. Новый траст.")
    public void shouldBuyReport() {
        basePageSteps.onHistoryPage().vinReportPreview().purchase().buttonContains(ONE_REPORT).click();
        basePageSteps.onLkSalesPage().switchToBillingFrame();

        basePageSteps.onLkSalesPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.onLkSalesPage().newTrust().title().waitUntil(hasText(REPORT_TITLE), 15);

        mockRule.overwriteStub(0,
                stub("desktop/CarfaxReportRawVinPaid")
                        .withPredicateType(DEEP_EQUALS)
                        .withRequestQuery(
                                query().setVinOrLicensePlate(VIN)
                                        .setDecrementQuota(true)));

        newTrustSteps.payWithCard();

        basePageSteps.onLkSalesPage().billingPopup().waitUntil(not(isDisplayed()));
        basePageSteps.onHistoryPage().vinReport().status().should(isDisplayed());
        basePageSteps.onHistoryPage().vinReport().repairCalculations().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class, Billing.class})
    @DisplayName("Выбор в биллинг попапе пакета из 5 отчетов, покупка. Новый траст.")
    public void shouldChoose5ReportsBundleAndBuy() {
        basePageSteps.onHistoryPage().vinReportPreview().purchase().buttonContains(ONE_REPORT).click();
        basePageSteps.onLkSalesPage().switchToBillingFrame();
        basePageSteps.onLkSalesPage().billingPopup().waitUntil(isDisplayed());

        mockRule.overwriteStub(7,
                stub().withPostDeepEquals(BILLING_AUTORU_PAYMENT_INIT)
                        .withRequestBody(
                                paymentInitHistoryRequest()
                                        .setSubscribePurchaseCount(5).getBody())
                        .withResponseBody(
                                paymentInitVasResponse()
                                        .setDetailedProductInfos(
                                                getHistoryReportProductInfo()
                                        ).getBody())
        );

        basePageSteps.onLkSalesPage().newTrust().reportBundleList().radioButtonContains("Пакет из 5 отчётов ПроАвто").click();

        mockRule.overwriteStub(0,
                stub("desktop/CarfaxReportRawVinPaid")
                        .withPredicateType(DEEP_EQUALS)
                        .withRequestQuery(
                                query().setVinOrLicensePlate(VIN)
                                        .setDecrementQuota(true)));

        newTrustSteps.payWithCard();

        basePageSteps.onLkSalesPage().billingPopup().waitUntil(not(isDisplayed()));
        basePageSteps.onHistoryPage().vinReport().status().should(isDisplayed());
        basePageSteps.onHistoryPage().vinReport().repairCalculations().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class, Billing.class})
    @DisplayName("Покупка 10 отчетов. Новый траст.")
    public void shouldBuyReportPackage() {
        mockRule.overwriteStub(7,
                stub().withPostDeepEquals(BILLING_AUTORU_PAYMENT_INIT)
                        .withRequestBody(
                                paymentInitHistoryRequest()
                                        .setSubscribePurchaseCount(10).getBody())
                        .withResponseBody(
                                paymentInitVasResponse()
                                        .setDetailedProductInfos(
                                                getHistoryReportProductInfo()
                                        ).getBody())
        );

        basePageSteps.onHistoryPage().vinReportPreview().purchase().buttonContains(BUY_PACKAGE).click();
        basePageSteps.onLkSalesPage().switchToBillingFrame();

        basePageSteps.onLkSalesPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.onLkSalesPage().newTrust().title().waitUntil(hasText(REPORT_TITLE), 15);
        basePageSteps.onLkSalesPage().newTrust().bundleDescription().waitUntil(hasText(REPORT_DESCRIPTION));

        mockRule.overwriteStub(0,
                stub("desktop/CarfaxReportRawVinPaid")
                        .withPredicateType(DEEP_EQUALS)
                        .withRequestQuery(
                                query().setVinOrLicensePlate(VIN)
                                        .setDecrementQuota(true)));

        newTrustSteps.payWithCard();

        basePageSteps.onLkSalesPage().billingPopup().waitUntil(not(isDisplayed()));
        basePageSteps.onHistoryPage().vinReport().status().should(isDisplayed());
        basePageSteps.onHistoryPage().vinReport().repairCalculations().should(isDisplayed());
    }

}
