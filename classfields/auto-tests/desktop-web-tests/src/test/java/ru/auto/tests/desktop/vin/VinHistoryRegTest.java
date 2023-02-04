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
import ru.auto.tests.desktop.categories.Billing;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopDevToolsTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
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
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.step.CookieSteps.FORCE_DISABLE_TRUST;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;

@Feature(VIN)
@DisplayName("Покупка отчёта под зарегом")
@GuiceModules(DesktopDevToolsTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class VinHistoryRegTest {

    private static final String VIN = "4S2CK58D924333406";
    private static final String VIN2 = "4S2CK58D924333407";
    private static final String LICENSE_PLATE = "Y151BB178";

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
    public SeleniumMockSteps seleniumMockSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() {
        cookieSteps.setExpFlags(FORCE_DISABLE_TRUST);

        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/CarfaxReportRawVinNotPaid"),
                stub("desktop/CarfaxReportRawLicensePlateNotPaid"),
                stub("desktop/BillingSubscriptionsOffersHistoryReportsPrices")).create();

        urlSteps.testing().path(HISTORY).open();
    }

    @Test
    @Category({Regression.class, Testing.class, Billing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Покупка пакета из 5 отчётов в сайдбаре")
    public void shouldBuy5ReportsPackageInSidebarPromo() {
        mockRule.delete();
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/BillingSubscriptionsOffersHistoryReportsPrices"),
                stub("desktop/BillingAutoruPaymentInitVinPackageSidebarPromo5Reports")).create();

        basePageSteps.scrollDown(500);
        basePageSteps.onHistoryPage().sidebar().vinPackagePromo().button("5 отчётов за 599\u00a0₽").hover()
                .click();
        basePageSteps.onHistoryPage().switchToBillingFrame();
        basePageSteps.onHistoryPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.onHistoryPage().billingPopup().header().waitUntil(hasText("Отчёт о проверке по VIN"));
        basePageSteps.onHistoryPage().billingPopup().subHeader()
                .waitUntil(hasText("Покупка пакета из 5 отчётов за 599 ₽. Действует 1 год"));

        mockRule.delete();
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/BillingAutoruPaymentProcessVinHistory"),
                stub("desktop/BillingAutoruPayment"),
                stub("desktop/BillingSubscriptionsOffersHistoryReportsPricesQuotaLeft5")).create();

        yaKassaSteps.payWithCard();
        yaKassaSteps.waitForSuccessMessage();
        basePageSteps.onHistoryPage().billingPopupCloseButton().click();
        basePageSteps.onHistoryPage().billingPopupFrame().waitUntil(not(isDisplayed()));
        basePageSteps.onHistoryPage().sidebar().packageQuotaInfo().waitUntil(hasText("Осталось 5 отчётов в пакете"));
    }

    @Test
    @Category({Regression.class, Testing.class, Billing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Покупка пакета из 10 отчётов в сайдбаре")
    public void shouldBuy10ReportsPackageInSidebarPromo() {
        mockRule.delete();
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/BillingSubscriptionsOffersHistoryReportsPrices"),
                stub("desktop/BillingAutoruPaymentInitVinPackageSidebarPromo10Reports")).create();

        basePageSteps.scrollDown(500);
        basePageSteps.onHistoryPage().sidebar().vinPackagePromo().button("10 отчётов за 990\u00a0₽").hover()
                .click();
        basePageSteps.onHistoryPage().switchToBillingFrame();
        basePageSteps.onHistoryPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.onHistoryPage().billingPopup().header().waitUntil(hasText("Отчёт о проверке по VIN"));
        basePageSteps.onHistoryPage().billingPopup().subHeader()
                .waitUntil(hasText("Покупка пакета из 10 отчётов за 990 ₽. Действует 1 год"));

        mockRule.delete();
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/BillingAutoruPaymentProcessVinHistory"),
                stub("desktop/BillingAutoruPayment"),
                stub("desktop/BillingSubscriptionsOffersHistoryReportsPricesQuotaLeft10")).create();

        yaKassaSteps.payWithCard();
        yaKassaSteps.waitForSuccessMessage();
        basePageSteps.onHistoryPage().billingPopupCloseButton().click();
        basePageSteps.onHistoryPage().billingPopupFrame().waitUntil(not(isDisplayed()));
        basePageSteps.onHistoryPage().sidebar().vinPackagePromo().waitUntil(not(isDisplayed()));
        basePageSteps.onHistoryPage().sidebar().packageQuotaInfo().waitUntil(hasText("Осталось 10 отчётов в пакете"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение оставшейся квоты в пакете (докупка не запрещена)")
    public void shouldSeePackageQuotaPurchaseAllowed() {
        mockRule.delete();
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/BillingSubscriptionsOffersHistoryReportsPricesQuotaLeft5")).create();

        urlSteps.testing().path(HISTORY).open();

        basePageSteps.onHistoryPage().sidebar().packageQuotaInfo().waitUntil(hasText("Осталось 5 отчётов в пакете"));
        basePageSteps.onHistoryPage().sidebar().button("5 отчётов за 599\u00a0₽").should(not(isDisplayed()));
        basePageSteps.onHistoryPage().sidebar().button("10 отчётов за 990\u00a0₽").should(isEnabled());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Просмотр разных отчётов при наличии купленного пакета")
    public void shouldSeeSingleReportsFromPackage() {
        mockRule.delete();
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/CarfaxReportRawVinNotPaidQuotaLeft5"),
                stub("desktop/CarfaxReportRawVinNotPaidQuotaLeft4"),
                stub("desktop/CarfaxReportRawVinPaidDecrementQuota"),
                stub("desktop/CarfaxReportRawVinPaidDecrementQuotaOtherVin"),
                stub("desktop/BillingSubscriptionsOffersHistoryReportsPrices")).create();

        urlSteps.testing().path(HISTORY).open();

        basePageSteps.onHistoryPage().topBlock().input("Госномер или VIN", VIN);
        basePageSteps.onHistoryPage().topBlock().button("Проверить").click();
        urlSteps.testing().path(HISTORY).path(VIN).path("/").shouldNotSeeDiff();
        basePageSteps.onHistoryPage().vinReportPreview().button("Смотреть полный отчётосталось 5\u00a0отчётов")
                .click();
        basePageSteps.onHistoryPage().vinReport().waitUntil(isDisplayed());
        basePageSteps.onHistoryPage().vinReport().status().waitUntil(isDisplayed())
                .should(hasText("Проверено 4 из 9 источников\nМы сообщим вам, как только отчёт будет полностью готов"));

        basePageSteps.onHistoryPage().sidebar().clearInputButton("Госномер или VIN").click();
        basePageSteps.onHistoryPage().sidebar().input("Госномер или VIN", VIN2);
        basePageSteps.onHistoryPage().sidebar().button("Проверить").click();
        urlSteps.testing().path(HISTORY).path(VIN2).path("/").shouldNotSeeDiff();
        basePageSteps.onHistoryPage().vinReportPreview().button("Смотреть полный отчётосталось 4\u00a0отчёта")
                .click();
        basePageSteps.onHistoryPage().vinReport().waitUntil(isDisplayed());
        basePageSteps.onHistoryPage().vinReport().status().waitUntil(isDisplayed())
                .should(hasText("Проверено 4 из 9 источников\nМы сообщим вам, как только отчёт будет полностью готов"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение оставшейся квоты в пакете (докупка запрещена)")
    public void shouldSeePackageQuotaPurchaseForbidden() {
        mockRule.delete();
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/BillingSubscriptionsOffersHistoryReportsPricesQuotaLeft5PurchaseForbidden")).create();

        urlSteps.testing().path(HISTORY).open();

        basePageSteps.onHistoryPage().sidebar().packageQuotaInfo().waitUntil(hasText("Осталось 5 отчётов в пакете"));
    }

    @Test
    @Category({Regression.class, Testing.class, Billing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Покупка одного отчёта (поиск по госномеру)")
    public void shouldBuySingleReportByLicensePlate() {
        basePageSteps.onHistoryPage().topBlock().input("Госномер или VIN", LICENSE_PLATE);
        basePageSteps.onHistoryPage().topBlock().button("Проверить").click();
        urlSteps.path(LICENSE_PLATE).path("/").shouldNotSeeDiff();
        basePageSteps.onHistoryPage().vinReportPreview().waitUntil(isDisplayed());

        mockRule.delete();
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/BillingSubscriptionsOffersHistoryReportsPricesLicensePlate"),
                stub("desktop/BillingAutoruPaymentInitLicensePlate"),
                stub("desktop/BillingAutoruPaymentProcessVinHistoryLicensePlate"),
                stub("desktop/BillingAutoruPayment"),
                stub("desktop/CarfaxReportRawLicensePlatePaidDecrementQuota")).create();

        basePageSteps.onHistoryPage().vinReportPreview().button("Один отчёт за 499\u00a0₽").click();
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
    @Category({Regression.class, Testing.class, Billing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Покупка пакета отчётов (поиск по VIN)")
    public void shouldBuyReportsPackageByVin() {
        basePageSteps.onHistoryPage().topBlock().input("Госномер или VIN", VIN);
        basePageSteps.onHistoryPage().topBlock().button("Проверить").click();
        urlSteps.path(VIN).path("/").shouldNotSeeDiff();
        basePageSteps.onHistoryPage().vinReportPreview().waitUntil(isDisplayed());

        mockRule.delete();
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/BillingSubscriptionsOffersHistoryReportsPricesVin"),
                stub("desktop/BillingAutoruPaymentInitVinPackageVin"),
                stub("desktop/BillingAutoruPaymentProcessVinHistoryVin"),
                stub("desktop/BillingAutoruPayment"),
                stub("desktop/CarfaxReportRawVinPaidDecrementQuota")).create();

        basePageSteps.onHistoryPage().vinReportPreview()
                .buttonContains("Купить пакет за 990\u00a0₽").click();
        basePageSteps.onHistoryPage().switchToBillingFrame();
        basePageSteps.onHistoryPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.onHistoryPage().billingPopup().header().waitUntil(hasText("Отчёт о проверке по VIN"));
        basePageSteps.onHistoryPage().billingPopup().subHeader()
                .waitUntil(hasText("Покупка пакета из 10 отчётов за 693 \u20BD"));
        yaKassaSteps.payWithCard();
        basePageSteps.onHistoryPage().billingPopupFrame().waitUntil(not(isDisplayed()));
        basePageSteps.onHistoryPage().vinReport().status().waitUntil(isDisplayed())
                .should(hasText("Проверено 4 из 9 источников\nМы сообщим вам, как только отчёт будет полностью готов"));
        basePageSteps.onHistoryPage().vinReport().waitUntil(isDisplayed());

        shouldSeeMetrics();
    }

    @Test
    @Category({Regression.class, Testing.class, Billing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Покупка пакета отчётов в поп-апе (поиск по VIN)")
    public void shouldBuyReportsPackageInPopupByVin() {
        basePageSteps.onHistoryPage().topBlock().input("Госномер или VIN", VIN);
        basePageSteps.onHistoryPage().topBlock().button("Проверить").click();
        urlSteps.path(VIN).path("/").shouldNotSeeDiff();
        basePageSteps.onHistoryPage().vinReportPreview().waitUntil(isDisplayed());

        mockRule.delete();
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/BillingSubscriptionsOffersHistoryReportsPricesVin"),
                stub("desktop/BillingAutoruPaymentInitVin"),
                stub("desktop/BillingAutoruPaymentInitVinPackageVin"),
                stub("desktop/BillingAutoruPaymentProcessVinHistoryVin"),
                stub("desktop/BillingAutoruPayment"),
                stub("desktop/CarfaxReportRawVinPaidDecrementQuota")).create();

        basePageSteps.onHistoryPage().vinReportPreview().button("Один отчёт за 499\u00a0₽").click();
        basePageSteps.onHistoryPage().switchToBillingFrame();
        basePageSteps.onHistoryPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.onHistoryPage().billingPopup().header().waitUntil(hasText("Отчёт о проверке по VIN"));
        basePageSteps.onHistoryPage().billingPopup()
                .vinSwitcher("Пакет из 10\u00a0отчётов99\u00a0₽ / отчётВыгода 29%990\u00a0₽").click();
        yaKassaSteps.payWithCard();
        basePageSteps.onHistoryPage().billingPopupFrame().waitUntil(not(isDisplayed()));
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
