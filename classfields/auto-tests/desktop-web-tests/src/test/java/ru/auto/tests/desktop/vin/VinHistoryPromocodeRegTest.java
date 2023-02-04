package ru.auto.tests.desktop.vin;

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
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopDevToolsTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.YaKassaSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.VIN;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.HISTORY;
import static ru.auto.tests.desktop.matchers.RequestHasQueryItemsMatcher.hasGoal;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneMetricsRequest;
import static ru.auto.tests.desktop.step.CookieSteps.FORCE_DISABLE_TRUST;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(VIN)
@DisplayName("Покупка отчёта с промокодом под зарегом")
@GuiceModules(DesktopDevToolsTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class VinHistoryPromocodeRegTest {

    private static final String VIN = "4S2CK58D924333406";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private YaKassaSteps yaKassaSteps;

    @Inject
    public SeleniumMockSteps seleniumMockSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() {
        cookieSteps.setExpFlags(FORCE_DISABLE_TRUST);

        mockRule.newMock().with("desktop/SessionAuthUser").post();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Купить отчёт» - промокод 50%")
    public void shouldClickBuyReportButtonPromocode50() {
        mockRule.with("desktop/CarfaxReportRawVinNotPaidDiscount",
                "desktop/BillingSubscriptionsOffersHistoryReportsPricesPromocodeDiscount50",
                "desktop/BillingSubscriptionsOffersHistoryReportsPricesVinPromocodeDiscount50",
                "desktop/BillingAutoruPaymentInitVin",
                "desktop/BillingAutoruPaymentProcessVinHistoryVin",
                "desktop/BillingAutoruPayment",
                "desktop/CarfaxReportRawVinPaidDecrementQuota").update();

        urlSteps.testing().path(HISTORY).open();
        basePageSteps.onHistoryPage().topBlock().input("Госномер или VIN", VIN);
        basePageSteps.onHistoryPage().topBlock().button("Проверить").click();
        basePageSteps.onHistoryPage().vinReportPreview().button("Один отчёт за 70\u00a0₽Вместо 139\u00a0₽")
                .click();
        basePageSteps.onHistoryPage().switchToBillingFrame();
        basePageSteps.onHistoryPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.onHistoryPage().billingPopup().header().waitUntil(hasText("Отчёт о проверке по VIN"));
        yaKassaSteps.payWithCard();
        basePageSteps.onHistoryPage().billingPopupFrame().waitUntil(not(isDisplayed()));
        basePageSteps.onHistoryPage().vinReport().waitUntil(isDisplayed());
        basePageSteps.onHistoryPage().vinReport().status().waitUntil(isDisplayed())
                .should(hasText("Проверено 4 из 9 источников\nМы сообщим вам, как только отчёт будет полностью готов"));

        shouldSeeMetrics();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Купить отчёт» - промокод 100%")
    public void shouldClickBuyReportButtonPromocode100() {
        mockRule.with("desktop/CarfaxReportRawVinNotPaidFree",
                "desktop/BillingSubscriptionsOffersHistoryReportsPricesPromocodeDiscount100",
                "desktop/BillingSubscriptionsOffersHistoryReportsPricesVinPromocodeDiscount100",
                "desktop/BillingAutoruPaymentInitVinFree",
                "desktop/BillingAutoruPaymentProcessFree",
                "desktop/BillingAutoruPayment",
                "desktop/CarfaxReportRawVinPaidDecrementQuota").update();

        urlSteps.testing().path(HISTORY).open();

        basePageSteps.onHistoryPage().topBlock().input("Госномер или VIN", VIN);
        basePageSteps.onHistoryPage().topBlock().button("Проверить").click();
        basePageSteps.onHistoryPage().vinReportPreview().button("Один отчёт бесплатноВместо 139\u00a0₽").click();
        basePageSteps.onHistoryPage().switchToBillingFrame();
        basePageSteps.onHistoryPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.onHistoryPage().billingPopup().header().waitUntil(hasText("Отчёт о проверке по VIN"));
        basePageSteps.switchToDefaultFrame();
        basePageSteps.onHistoryPage().billingPopupFrame().waitUntil(not(isDisplayed()));
        basePageSteps.onHistoryPage().vinReport().waitUntil(isDisplayed());
        basePageSteps.onHistoryPage().vinReport().status().waitUntil(isDisplayed())
                .should(hasText("Проверено 4 из 9 источников\nМы сообщим вам, как только отчёт будет полностью готов"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Покупка одного отчёта (поиск по VIN) - промокод 50%")
    public void shouldBuySingleReportByVinPromocode50() {
        mockRule.with("desktop/CarfaxReportRawVinNotPaidDiscount",
                "desktop/BillingSubscriptionsOffersHistoryReportsPricesPromocodeDiscount50",
                "desktop/BillingSubscriptionsOffersHistoryReportsPricesVinPromocodeDiscount50",
                "desktop/BillingAutoruPaymentInitVin",
                "desktop/BillingAutoruPaymentProcessVinHistoryVin",
                "desktop/BillingAutoruPayment",
                "desktop/CarfaxReportRawVinPaidDecrementQuota").update();

        urlSteps.testing().path(HISTORY).open();

        basePageSteps.onHistoryPage().topBlock().input("Госномер или VIN", VIN);
        basePageSteps.onHistoryPage().topBlock().button("Проверить").click();
        basePageSteps.onHistoryPage().vinReportPreview().waitUntil(isDisplayed());
        basePageSteps.onHistoryPage().vinReportPreview().button("Один отчёт за 70\u00a0₽Вместо 139\u00a0₽")
                .click();
        basePageSteps.onHistoryPage().switchToBillingFrame();
        basePageSteps.onHistoryPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.onHistoryPage().billingPopup().header().waitUntil(hasText("Отчёт о проверке по VIN"));
        yaKassaSteps.payWithCard();
        basePageSteps.onHistoryPage().billingPopupFrame().waitUntil(not(isDisplayed()));
        basePageSteps.onHistoryPage().vinReport().waitUntil(isDisplayed());
        basePageSteps.onHistoryPage().vinReport().status().waitUntil(isDisplayed())
                .should(hasText("Проверено 4 из 9 источников\nМы сообщим вам, как только отчёт будет полностью готов"));

        shouldSeeMetrics();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Покупка одного отчёта (поиск по VIN) - промокод 100%")
    public void shouldBuySingleReportByVinPromocode100() {
        mockRule.with("desktop/CarfaxReportRawVinNotPaidFree",
                "desktop/BillingSubscriptionsOffersHistoryReportsPricesPromocodeDiscount100",
                "desktop/BillingSubscriptionsOffersHistoryReportsPricesVinPromocodeDiscount100",
                "desktop/BillingAutoruPaymentInitVinFree",
                "desktop/BillingAutoruPaymentProcessFree",
                "desktop/BillingAutoruPayment",
                "desktop/CarfaxReportRawVinPaidDecrementQuota").update();

        urlSteps.testing().path(HISTORY).open();

        basePageSteps.onHistoryPage().topBlock().input("Госномер или VIN", VIN);
        basePageSteps.onHistoryPage().topBlock().button("Проверить").click();
        basePageSteps.onHistoryPage().vinReportPreview().waitUntil(isDisplayed());
        basePageSteps.onHistoryPage().vinReportPreview().button("Один отчёт бесплатно").click();
        basePageSteps.onHistoryPage().switchToBillingFrame();
        basePageSteps.onHistoryPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.onHistoryPage().billingPopup().header().waitUntil(hasText("Отчёт о проверке по VIN"));
        //basePageSteps.onHistoryPage().billingPopup().vinSwitcher().should(not(isDisplayed()));
        basePageSteps.switchToDefaultFrame();
        //basePageSteps.onHistoryPage().billingPopupCloseButton().click();
        basePageSteps.onHistoryPage().billingPopupFrame().waitUntil(not(isDisplayed()));
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
