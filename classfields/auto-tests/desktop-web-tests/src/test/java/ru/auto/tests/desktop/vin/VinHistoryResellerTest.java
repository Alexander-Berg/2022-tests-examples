package ru.auto.tests.desktop.vin;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.YaKassaSteps;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.VIN;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.HISTORY;
import static ru.auto.tests.desktop.step.CookieSteps.FORCE_DISABLE_TRUST;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Покупка отчёта под перекупом")
@Feature(VIN)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
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
    private YaKassaSteps yaKassaSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() {
        cookieSteps.setExpFlags(FORCE_DISABLE_TRUST);

        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/CarfaxReportRawVinNotPaidReseller",
                "desktop/CarfaxReportRawLicensePlateNotPaid").post();

        urlSteps.testing().path(HISTORY).open();
    }

    @Test
    @Category({Regression.class, Testing.class, Billing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Покупка одного отчёта (поиск по VIN)")
    public void shouldBuySingleReportByVin() {
        basePageSteps.onHistoryPage().topBlock().input("Госномер или VIN", VIN);
        basePageSteps.onHistoryPage().button("Проверить").click();
        basePageSteps.onHistoryPage().vinReportPreview().waitUntil("Превью отчёта не появилось", isDisplayed(), 10);

        mockRule.delete();
        mockRule.newMock().with("desktop/OfferCarsUsedUser",
                "desktop/SessionAuthUser",
                "desktop/BillingSubscriptionsOffersHistoryReportsPricesVinReseller",
                "desktop/BillingAutoruPaymentInitVin",
                "desktop/BillingAutoruPaymentProcessVinHistoryVin",
                "desktop/BillingAutoruPayment",
                "desktop/CarfaxReportRawVinPaidDecrementQuota").post();

        basePageSteps.onHistoryPage().vinReportPreview().button("Один отчёт за 139\u00a0₽").click();
        basePageSteps.onHistoryPage().switchToBillingFrame();
        basePageSteps.onHistoryPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.onHistoryPage().billingPopup().header().waitUntil(hasText("Отчёт о проверке по VIN"));
        yaKassaSteps.payWithCard();
        basePageSteps.onHistoryPage().billingPopupFrame().waitUntil(not(isDisplayed()));
        basePageSteps.onHistoryPage().vinReport().status().waitUntil(isDisplayed())
                .should(hasText("Проверено 4 из 9 источников\nМы сообщим вам, как только отчёт будет полностью готов"));
        basePageSteps.onHistoryPage().vinReport().waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class, Billing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Покупка пакета отчётов (поиск по VIN)")
    public void shouldBuyReportsPackageByVin() {
        basePageSteps.onHistoryPage().topBlock().input("Госномер или VIN", VIN);
        basePageSteps.onHistoryPage().topBlock().button("Проверить").click();
        basePageSteps.onHistoryPage().vinReportPreview().waitUntil(isDisplayed());

        mockRule.delete();
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/BillingSubscriptionsOffersHistoryReportsPricesVinReseller",
                "desktop/BillingAutoruPaymentInitVinPackageVinReseller",
                "desktop/BillingAutoruPaymentProcessVinHistoryVin",
                "desktop/BillingAutoruPayment",
                "desktop/CarfaxReportRawVinPaidDecrementQuota").post();

        basePageSteps.onHistoryPage().vinReportPreview()
                .button("Купить пакет за 2\u00a0990\u00a0₽").click();
        basePageSteps.onHistoryPage().switchToBillingFrame();
        basePageSteps.onHistoryPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.onHistoryPage().billingPopup().header().waitUntil(hasText("Отчёт о проверке по VIN"));
        basePageSteps.onCardPage().billingPopup().subHeader()
                .should(hasText("Покупка пакета из 50 отчётов за 2 990 ₽. Действует 32 дня"));
        yaKassaSteps.payWithCard();
        //yaKassaSteps.waitForSuccessMessage();
        //basePageSteps.onHistoryPage().billingPopupCloseButton().click();
        basePageSteps.onHistoryPage().billingPopupFrame().waitUntil(not(isDisplayed()));
        basePageSteps.onHistoryPage().vinReport().status().waitUntil(isDisplayed())
                .should(hasText("Проверено 4 из 9 источников\nМы сообщим вам, как только отчёт будет полностью готов"));
        basePageSteps.onHistoryPage().vinReport().waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class, Billing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Покупка пакета отчётов в поп-апе (поиск по VIN)")
    public void shouldBuyReportsPackageInPopupByVin() {
        basePageSteps.onHistoryPage().topBlock().input("Госномер или VIN", VIN);
        basePageSteps.onHistoryPage().topBlock().button("Проверить").click();
        basePageSteps.onHistoryPage().vinReportPreview().waitUntil(isDisplayed());

        mockRule.delete();
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/BillingSubscriptionsOffersHistoryReportsPricesVinReseller",
                "desktop/BillingAutoruPaymentInitVin",
                "desktop/BillingAutoruPaymentInitVinPackageVinReseller",
                "desktop/BillingAutoruPaymentProcessVinHistoryVin",
                "desktop/BillingAutoruPayment",
                "desktop/CarfaxReportRawVinPaidDecrementQuota").post();

        basePageSteps.onHistoryPage().vinReportPreview().button("Один отчёт за 139\u00a0₽").click();
        basePageSteps.onHistoryPage().switchToBillingFrame();
        basePageSteps.onHistoryPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.onHistoryPage().billingPopup().header().waitUntil(hasText("Отчёт о проверке по VIN"));
        basePageSteps.onHistoryPage().billingPopup()
                .vinSwitcher("Пакет из 50\u00a0отчётов\u00a0•\u00a0действует 32\u00a0дня60\u00a0₽ / отчёт" +
                        "Выгода 85%2\u00a0990\u00a0₽").click();
        yaKassaSteps.payWithCard();
        basePageSteps.onHistoryPage().billingPopupFrame().waitUntil(not(isDisplayed()));
        basePageSteps.onHistoryPage().vinReport().status().waitUntil(isDisplayed())
                .should(hasText("Проверено 4 из 9 источников\nМы сообщим вам, как только отчёт будет полностью готов"));
        basePageSteps.onHistoryPage().vinReport().waitUntil(isDisplayed());
    }
}
