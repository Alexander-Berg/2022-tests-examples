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
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.VIN;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.HISTORY;
import static ru.auto.tests.desktop.matchers.RequestHasQueryItemsMatcher.hasGoal;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneMetricsRequest;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(VIN)
@DisplayName("Покупка отчёта с промокодом под зарегом")
@GuiceModules(MobileDevToolsTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class VinHistoryPromocodeRegTest {

    private static final String VIN = "4S2CK58D924333406";
    private static final String VIN2 = "4S2CK58D924333407";
    private static final String LICENSE_PLATE = "Y151BB178";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    public SeleniumMockSteps seleniumMockSteps;

    @Inject
    public PaymentSteps paymentSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser").post();
    }

    @Test
    @Category({Regression.class, Testing.class, Billing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Покупка одного отчёта (поиск по VIN) - промокод 50%")
    public void shouldBuySingleReportByVinPromocode50() {
        mockRule.with("desktop/CarfaxReportRawVinNotPaidDiscount",
                "desktop/BillingSubscriptionsOffersHistoryReportsPricesPromocodeDiscount50",
                "desktop/BillingSubscriptionsOffersHistoryReportsPricesVinPromocodeDiscount50",
                "mobile/BillingAutoruPaymentInitVinHistory",
                "mobile/BillingAutoruPaymentProcessVinHistory",
                "desktop/BillingAutoruPayment",
                "desktop/CarfaxReportRawVinPaidDecrementQuota").update();

        urlSteps.testing().path(HISTORY).open();

        basePageSteps.onHistoryPage().input("Госномер или VIN", VIN);
        basePageSteps.onHistoryPage().findButton().click();
        basePageSteps.onHistoryPage().vinReportPreview().waitUntil(isDisplayed());
        basePageSteps.onHistoryPage().vinReportPreview().button("Один отчёт за 70\u00a0₽Вместо 139\u00a0₽").click();
        basePageSteps.onHistoryPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.switchToPaymentMethodsFrame();
        basePageSteps.onHistoryPage().paymentMethodsFrameContent().title()
                .waitUntil(hasText("Отчёт о проверке по VIN"));
        basePageSteps.switchToDefaultFrame();
        basePageSteps.selectPaymentMethod("Банковская карта");

        basePageSteps.clickPayButton();
        paymentSteps.payByCard();
        basePageSteps.onHistoryPage().billingPopup().waitUntil(not(isDisplayed()));
        basePageSteps.onHistoryPage().vinReport().status().waitUntil(isDisplayed())
                .should(hasText("Проверено 4 из 9 источников\nМы сообщим вам, как только отчёт будет полностью готов"));
        basePageSteps.onHistoryPage().vinReport().waitUntil(isDisplayed());

        shouldSeeMetrics();
    }

    @Test
    @Category({Regression.class, Testing.class, Billing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Покупка одного отчёта (поиск по VIN) - промокод 100%")
    public void shouldBuySingleReportByVinPromocode100() {
        mockRule.with("desktop/CarfaxReportRawVinNotPaidFree",
                "desktop/BillingSubscriptionsOffersHistoryReportsPricesPromocodeDiscount100",
                "desktop/BillingSubscriptionsOffersHistoryReportsPricesVinPromocodeDiscount100",
                "mobile/BillingAutoruPaymentInitVinFree",
                "desktop/BillingAutoruPaymentProcessFree",
                "desktop/BillingAutoruPayment",
                "desktop/CarfaxReportRawVinPaidDecrementQuota").update();

        urlSteps.testing().path(HISTORY).open();

        basePageSteps.onHistoryPage().input("Госномер или VIN", VIN);
        basePageSteps.onHistoryPage().findButton().click();
        basePageSteps.onHistoryPage().vinReportPreview().waitUntil(isDisplayed());
        basePageSteps.onHistoryPage().vinReportPreview().button("Один отчёт бесплатно").click();
        waitSomething(5, TimeUnit.SECONDS);
        basePageSteps.onHistoryPage().billingPopup().waitUntil(not(isDisplayed()));
        basePageSteps.onHistoryPage().vinReport().waitUntil(isDisplayed());
        basePageSteps.onHistoryPage().vinReport().status().waitUntil(isDisplayed())
                .should(hasText("Проверено 4 из 9 источников\nМы сообщим вам, как только отчёт будет полностью готов"));

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
