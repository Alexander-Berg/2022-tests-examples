package ru.auto.tests.desktop.vin;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
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
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.YaKassaSteps;

import java.util.concurrent.TimeUnit;

import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.VIN;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.HISTORY;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.matchers.RequestHasQueryItemsMatcher.hasGoal;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneMetricsRequest;
import static ru.auto.tests.desktop.step.CookieSteps.FORCE_DISABLE_TRUST;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Покупка отчёта с промокодом под зарегом")
@Feature(VIN)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopDevToolsTestsModule.class)
public class VinSaleRegPromocodeTest {

    private static final String SALE_ID = "/1076842087-f1e84/";
    private static final int SCROLL = 1000;
    private String saleUrl;

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
    public SeleniumMockSteps seleniumMockSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() {
        cookieSteps.setExpFlags(FORCE_DISABLE_TRUST);
    }

    @Test
    @Category({Regression.class, Testing.class, Billing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Покупка одного отчёта - промокод 50%")
    public void shouldBuySingleReportPromocode50() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/OfferCarsUsedUser",
                "desktop/CarfaxOfferCarsRawNotPaidDiscount",
                "desktop/CarfaxOfferCarsRawPaidDecrementQuota",
                "desktop/BillingSubscriptionsOffersHistoryReportsPricesOfferPromocodeDiscount50",
                "desktop/BillingAutoruPaymentInitVinSale",
                "desktop/BillingAutoruPaymentInitVinDiscount",
                "desktop/BillingAutoruPaymentProcess",
                "desktop/BillingAutoruPayment").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
        saleUrl = urlSteps.getCurrentUrl();
        basePageSteps.scrollDown(SCROLL);
        basePageSteps.onCardPage().vinReport().waitUntil(isDisplayed());

        waitSomething(3, TimeUnit.SECONDS);
        mockRule.overwriteStub(2, "desktop/CarfaxOfferCarsRawPaid");

        basePageSteps.onCardPage().vinReport().button("Купить отчёт от 30\u00a0₽").click();
        basePageSteps.onCardPage().switchToBillingFrame();
        basePageSteps.onCardPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.onCardPage().billingPopup()
                .vinSwitcher("Один отчёт250\u00a0₽ / отчёт499\u00a0₽250\u00a0₽").click();
        basePageSteps.onCardPage().billingPopup()
                .vinSwitcherSelected("Один отчёт250\u00a0₽ / отчёт499\u00a0₽250\u00a0₽").should(isDisplayed());
        basePageSteps.onCardPage().billingPopup().checkbox("Запомнить карту").hover().click();
        yaKassaSteps.payWithCard();
        urlSteps.testing().path(HISTORY).path(SALE_ID).shouldNotSeeDiff();

        shouldSeeMetrics();
    }

    @Test
    @Category({Regression.class, Testing.class, Billing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Покупка одного отчёта - промокод 100%")
    public void shouldBuySingleReportPromoCode100() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/OfferCarsUsedUser",
                "desktop/CarfaxOfferCarsRawNotPaidFree",
                "desktop/BillingSubscriptionsOffersHistoryReportsPricesOfferPromocodeDiscount100",
                "desktop/BillingAutoruPaymentInitVinSaleFree",
                "desktop/BillingAutoruPaymentProcessFree",
                "desktop/BillingAutoruPayment").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
        saleUrl = urlSteps.getCurrentUrl();
        basePageSteps.scrollDown(SCROLL);
        basePageSteps.onCardPage().vinReport().waitUntil(isDisplayed());

        waitSomething(3, TimeUnit.SECONDS);
        mockRule.overwriteStub(2, "desktop/CarfaxOfferCarsRawPaid");

        basePageSteps.onCardPage().vinReport().button("Один отчёт бесплатно").click();
        urlSteps.testing().path(HISTORY).path(SALE_ID).shouldNotSeeDiff();
        basePageSteps.onHistoryPage().vinReport().waitUntil(isDisplayed());
        basePageSteps.onHistoryPage().vinReport().status().waitUntil(isDisplayed())
                .should(hasText("Проверено 8 из 9 источников\nМы сообщим вам, как только отчёт будет полностью готов"));

        shouldSeeMetrics();
    }

    @Step("Проверяем метрики")
    private void shouldSeeMetrics() {
        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasGoal(
                seleniumMockSteps.formatGoal("CARS_OPEN_REPORT_CARD"),
                saleUrl
        )));
    }
}
