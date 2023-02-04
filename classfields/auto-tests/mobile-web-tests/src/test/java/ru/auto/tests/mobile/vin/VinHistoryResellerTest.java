package ru.auto.tests.mobile.vin;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.mobile.step.PaymentSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.VIN;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.HISTORY;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Покупка отчёта под перекупом")
@Feature(VIN)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class VinHistoryResellerTest {

    private static final String VIN = "4S2CK58D924333406";
    private static final String LICENSE_PLATE = "Y151BB178";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private PaymentSteps paymentSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/CarfaxReportRawVinNotPaidReseller",
                "desktop/CarfaxReportRawLicensePlateNotPaid").post();

        urlSteps.testing().path(HISTORY).open();
    }

    @Test
    @Category({Regression.class, Testing.class, Billing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Покупка одного отчёта (VIN)")
    public void shouldBuySingleReportByVin() {
        basePageSteps.onHistoryPage().input("Госномер или VIN", VIN);
        basePageSteps.onHistoryPage().findButton().click();
        basePageSteps.onHistoryPage().vinReportPreview().waitUntil("Превью отчёта не появилось", isDisplayed(), 10);

        mockRule.delete();
        mockRule.newMock().with("desktop/OfferCarsUsedUser",
                "desktop/SessionAuthUser",
                "desktop/BillingSubscriptionsOffersHistoryReportsPricesVinReseller",
                "mobile/BillingAutoruPaymentInitVinHistory",
                "mobile/BillingAutoruPaymentProcessVinHistory",
                "desktop/BillingAutoruPayment",
                "desktop/CarfaxReportRawVinPaidDecrementQuota").post();

        basePageSteps.onHistoryPage().vinReportPreview().button("Полный отчёт за 139\u00a0₽").hover().click();
        basePageSteps.onHistoryPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.selectPaymentMethod("Банковская карта");
        basePageSteps.clickPayButton();
        paymentSteps.payByCard();
        paymentSteps.waitForSuccessMessage();
        basePageSteps.onHistoryPage().billingPopup().waitUntil(not(isDisplayed()));
        basePageSteps.onHistoryPage().vinReport().status().waitUntil(isDisplayed())
                .should(hasText("Проверено 4 из 9 источников\nМы сообщим вам, как только отчёт будет полностью готов"));
        basePageSteps.onHistoryPage().vinReport().waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class, Billing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Покупка пакета отчётов (VIN)")
    public void shouldBuyReportsPackageByVin() {
        basePageSteps.onHistoryPage().input("Госномер или VIN", VIN);
        basePageSteps.onHistoryPage().findButton().click();
        basePageSteps.onHistoryPage().vinReportPreview().waitUntil("Превью отчёта не появилось", isDisplayed(), 10);

        mockRule.delete();
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/BillingSubscriptionsOffersHistoryReportsPricesVinReseller",
                "mobile/BillingAutoruPaymentInitVinPackageVinReseller",
                "mobile/BillingAutoruPaymentProcessVinHistory",
                "desktop/BillingAutoruPayment",
                "desktop/CarfaxReportRawVinPaidDecrementQuota").post();

        basePageSteps.onHistoryPage().vinReportPreview()
                .vinPackage("50\u00a0отчётовВыгода 57%60\u00a0₽ / шт.2\u00a0990\u00a0₽").hover().click();
        basePageSteps.onHistoryPage().billingPopup().waitUntil(isDisplayed());

        basePageSteps.selectPaymentMethod("Банковская карта");
        basePageSteps.clickPayButton();
        paymentSteps.payByCard();
        paymentSteps.waitForSuccessMessage();
        basePageSteps.onHistoryPage().billingPopup().waitUntil(not(isDisplayed()));
        basePageSteps.onHistoryPage().vinReport().status().waitUntil(isDisplayed())
                .should(hasText("Проверено 4 из 9 источников\nМы сообщим вам, как только отчёт будет полностью готов"));
        basePageSteps.onHistoryPage().vinReport().waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class, Billing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Покупка пакета отчётов в поп-апе (VIN)")
    public void shouldBuyReportsPackageInPopupByVin() {
        basePageSteps.onHistoryPage().input("Госномер или VIN", VIN);
        basePageSteps.onHistoryPage().findButton().click();
        basePageSteps.onHistoryPage().vinReportPreview().waitUntil("Превью отчёта не появилось", isDisplayed(), 10);

        mockRule.delete();
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/BillingSubscriptionsOffersHistoryReportsPricesVinReseller",
                "mobile/BillingAutoruPaymentInitVinHistory",
                "mobile/BillingAutoruPaymentInitVinPackageVinReseller",
                "mobile/BillingAutoruPaymentProcessVinHistory",
                "desktop/BillingAutoruPayment",
                "desktop/CarfaxReportRawVinPaidDecrementQuota").post();

        basePageSteps.onHistoryPage().vinReportPreview().button("Полный отчёт за 139\u00a0₽").click();
        basePageSteps.onHistoryPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.selectVinPackage("50\u00a0отчётов\u00a0•\u00a0действует 32\u00a0дня60\u00a0₽ / шт." +
                "−85%2\u00a0990\u00a0₽");
        basePageSteps.selectPaymentMethod("Банковская карта");
        basePageSteps.clickPayButton();
        paymentSteps.payByCard();
        paymentSteps.waitForSuccessMessage();
        basePageSteps.onHistoryPage().billingPopup().waitUntil(not(isDisplayed()));
        basePageSteps.onHistoryPage().vinReport().status().waitUntil(isDisplayed())
                .should(hasText("Проверено 4 из 9 источников\nМы сообщим вам, как только отчёт будет полностью готов"));
        basePageSteps.onHistoryPage().vinReport().waitUntil(isDisplayed());
    }
}
