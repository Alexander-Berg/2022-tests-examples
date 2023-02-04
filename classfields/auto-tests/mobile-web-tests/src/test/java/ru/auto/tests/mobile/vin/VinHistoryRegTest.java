package ru.auto.tests.mobile.vin;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
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
import ru.auto.tests.desktop.module.MobileDevToolsTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.VIN;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.HISTORY;
import static ru.auto.tests.desktop.matchers.RequestHasQueryItemsMatcher.hasGoal;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneMetricsRequest;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(VIN)
@DisplayName("Про авто - покупка истории автомобиля под зарегом")
@GuiceModules(MobileDevToolsTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class VinHistoryRegTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private PaymentSteps paymentSteps;

    @Inject
    public SeleniumMockSteps seleniumMockSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/CarfaxReportRawVinNotPaid",
                "desktop/CarfaxReportRawLicensePlateNotPaid",
                "desktop/BillingSubscriptionsOffersHistoryReportsPrices").post();

        urlSteps.testing().path(HISTORY).open();
    }

    @Test
    @Category({Regression.class, Testing.class, Billing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Покупка пакета из 10 отчётов")
    public void shouldBuy10ReportsPackage() {
        mockRule.delete();
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/BillingSubscriptionsOffersHistoryReportsPrices",
                "mobile/BillingAutoruPaymentInitVinHistory10ReportsPackage").post();

        basePageSteps.onHistoryPage().vinPackagePromo()
                .should(hasText("Пакеты отчётов\nПакет отчётов — проверяйте автомобили с выгодой до 33%\n5 отчётов\n" +
                        "120 ₽ / шт.\n599 ₽\n10 отчётов\n99 ₽ / шт.\n990 ₽"));
        basePageSteps.onHistoryPage().vinPackagePromo().button("10\u00a0отчётов").click();
        basePageSteps.onHistoryPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.switchToPaymentMethodsFrame();
        basePageSteps.onHistoryPage().paymentMethodsFrameContent().title()
                .waitUntil(hasText("Отчёт о проверке по VIN"));
        basePageSteps.onCardPage().paymentMethodsFrameContent().subTitle()
                .waitUntil(hasText("Пакет из 10 отчётов. Действует 1 год"));
        basePageSteps.switchToDefaultFrame();
        basePageSteps.selectPaymentMethod("Банковская карта");

        mockRule.delete();
        mockRule.newMock().with("desktop/SessionAuthUser",
                "mobile/BillingAutoruPaymentProcessVinHistory2",
                "desktop/BillingAutoruPayment",
                "desktop/BillingSubscriptionsOffersHistoryReportsPricesQuotaLeft10").post();

        basePageSteps.clickPayButton();
        paymentSteps.payByCard();
        paymentSteps.waitForSuccessMessage();
        basePageSteps.onHistoryPage().billingPopup().waitUntil(not(isDisplayed()));
        basePageSteps.onHistoryPage().vinPackagePromo()
                .waitUntil(hasText("Оставшиеся отчеты\nВ вашем пакете осталось 10 отчётов")).click();
        basePageSteps.onHistoryPage().billingPopup().should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение оставшейся квоты в пакете (докупка не запрещена)")
    public void shouldSeePackageQuotaPurchaseAllowed() {
        mockRule.delete();
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/BillingSubscriptionsOffersHistoryReportsPricesQuotaLeft5").post();

        urlSteps.testing().path(HISTORY).open();

        basePageSteps.onHistoryPage().vinPackagePromo()
                .should(hasText("Пакеты отчётов\nПакет отчётов — проверяйте автомобили с выгодой до 0%\n10 отчётов\n" +
                        "99 ₽ / шт.\n990 ₽\nОставшиеся отчеты\nВ вашем пакете осталось 5 отчётов"));
        basePageSteps.onHistoryPage().vinPackagePromo().button("10\u00a0отчётов").click();
        basePageSteps.onHistoryPage().billingPopup().should((isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение оставшейся квоты в пакете (докупка запрещена)")
    public void shouldSeePackageQuotaPurchaseForbidden() {
        mockRule.delete();
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/BillingSubscriptionsOffersHistoryReportsPricesQuotaLeft5PurchaseForbidden").post();

        urlSteps.testing().path(HISTORY).open();

        basePageSteps.onHistoryPage().vinPackagePromo()
                .waitUntil(hasText("Оставшиеся отчеты\nВ вашем пакете осталось 5 отчётов")).click();
        basePageSteps.onHistoryPage().billingPopup().should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class, Billing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Покупка одного отчёта (поиск по VIN)")
    public void shouldBuySingleReportByVin() {
        basePageSteps.onHistoryPage().input("Госномер или VIN", "4S2CK58D924333406");
        basePageSteps.onHistoryPage().findButton().click();
        basePageSteps.onHistoryPage().vinReportPreview().waitUntil(isDisplayed());

        mockRule.delete();
        mockRule.newMock().with("desktop/SessionAuthUser",
                "mobile/BillingAutoruPaymentInitVinHistory",
                "mobile/BillingAutoruPaymentProcessVinHistory",
                "desktop/BillingAutoruPayment",
                "desktop/CarfaxReportRawVinPaidDecrementQuota").post();

        basePageSteps.onHistoryPage().vinReportPreview().button("Полный отчёт за 499\u00a0₽").click();
        basePageSteps.onHistoryPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.selectPaymentMethod("Банковская карта");
        basePageSteps.clickPayButton();
        paymentSteps.payByCard();
        paymentSteps.waitForSuccessMessage();
        basePageSteps.onHistoryPage().billingPopup().waitUntil(not(isDisplayed()));
        basePageSteps.onHistoryPage().vinReport().status().waitUntil(isDisplayed())
                .should(hasText("Проверено 4 из 9 источников\nМы сообщим вам, как только отчёт будет полностью готов"));
        basePageSteps.onHistoryPage().vinReport().waitUntil(isDisplayed());

        shouldSeeMetrics();
    }

    @Test
    @Category({Regression.class, Testing.class, Billing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Покупка одного отчёта (поиск по госномеру)")
    public void shouldBuySingleReportByLicensePlate() {
        basePageSteps.onHistoryPage().input("Госномер или VIN", "Y151BB178");
        basePageSteps.onHistoryPage().findButton().click();
        basePageSteps.onHistoryPage().vinReportPreview().waitUntil(isDisplayed());

        mockRule.delete();
        mockRule.newMock().with("desktop/SessionAuthUser",
                "mobile/BillingAutoruPaymentInitLicensePlate",
                "mobile/BillingAutoruPaymentProcessVinHistoryLicensePlate",
                "desktop/BillingAutoruPayment",
                "desktop/CarfaxReportRawLicensePlatePaidDecrementQuota").post();

        basePageSteps.onHistoryPage().vinReportPreview().button("Полный отчёт за 499\u00a0₽").click();
        basePageSteps.onHistoryPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.selectPaymentMethod("Банковская карта");
        basePageSteps.clickPayButton();
        paymentSteps.payByCard();
        paymentSteps.waitForSuccessMessage();
        basePageSteps.onHistoryPage().billingPopup().waitUntil(not(isDisplayed()));
        basePageSteps.onHistoryPage().vinReport().status().waitUntil(isDisplayed())
                .should(hasText("Проверено 4 из 9 источников\nМы сообщим вам, как только отчёт будет полностью готов"));
        basePageSteps.onHistoryPage().vinReport().waitUntil(isDisplayed());

        shouldSeeMetrics();
    }

    @Test
    @Category({Regression.class, Testing.class, Billing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Покупка пакета из 5 отчётов (поиск по VIN)")
    public void shouldBuy5ReportsPackageByVin() {
        basePageSteps.onHistoryPage().input("Госномер или VIN", "4S2CK58D924333406");
        basePageSteps.onHistoryPage().findButton().click();
        basePageSteps.onHistoryPage().vinReportPreview().waitUntil(isDisplayed());

        mockRule.delete();
        mockRule.newMock().with("desktop/SessionAuthUser",
                "mobile/BillingAutoruPaymentInitVinHistory5ReportsPackage",
                "mobile/BillingAutoruPaymentProcessVinHistory",
                "desktop/BillingAutoruPayment",
                "desktop/CarfaxReportRawVinPaidDecrementQuota").post();

        basePageSteps.onHistoryPage().vinReportPreview()
                .vinPackage("5\u00a0отчётовВыгода 76%120\u00a0₽ / шт.599\u00a0₽").hover().click();
        basePageSteps.onHistoryPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.selectPaymentMethod("Банковская карта");
        basePageSteps.clickPayButton();
        paymentSteps.payByCard();
        paymentSteps.waitForSuccessMessage();
        basePageSteps.onHistoryPage().billingPopup().waitUntil(not(isDisplayed()));
        basePageSteps.onHistoryPage().vinReport().status().waitUntil(isDisplayed())
                .should(hasText("Проверено 4 из 9 источников\nМы сообщим вам, как только отчёт будет полностью готов"));
        basePageSteps.onHistoryPage().vinReport().waitUntil(isDisplayed());

        shouldSeeMetrics();
    }

    @Step("Проверяем метрики")
    private void shouldSeeMetrics() {
        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasGoal(
                seleniumMockSteps.formatGoal("CARS_OPEN_REPORT_HISTORY"),
                urlSteps.getCurrentUrl()
        )));
    }
}
