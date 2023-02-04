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

@DisplayName("Объявление - блок «Отчёт об автомобиле» в галерее под зарегом")
@Feature(VIN)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopDevToolsTestsModule.class)
public class VinSaleGalleryRegTest {

    private static final String SALE_ID = "/1076842087-f1e84/";
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
    private UrlSteps urlSteps;

    @Inject
    public SeleniumMockSteps seleniumMockSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() {
        cookieSteps.setExpFlags(FORCE_DISABLE_TRUST);

        mockRule.newMock().with("desktop/CarfaxOfferCarsRawNotPaid",
                "desktop/SessionAuthUser",
                "desktop/OfferCarsUsedUser",
                "desktop/BillingAutoruPaymentInitVinSaleGallery",
                "desktop/BillingAutoruPaymentProcess",
                "desktop/BillingAutoruPayment").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
        saleUrl = urlSteps.getCurrentUrl();

        basePageSteps.setWideWindowSize(3000);
        basePageSteps.waitForVinBlock();

        mockRule.overwriteStub(0, "desktop/CarfaxOfferCarsRawPaid");
    }

    @Test
    @Category({Regression.class, Testing.class, Billing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Покупка одного отчёта")
    public void shouldBuySingleReport() {
        basePageSteps.onCardPage().fullScreenGallery().vinReport().button("Один отчёт за 499\u00a0₽").click();
        basePageSteps.onCardPage().switchToBillingFrame();
        basePageSteps.onCardPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.onCardPage().billingPopup().checkbox("Запомнить карту").click();
        basePageSteps.onCardPage().billingPopup().header().waitUntil(hasText("Отчёт о проверке по VIN"));
        basePageSteps.onCardPage().billingPopup().priceHeader().waitUntil(hasText("69 ₽"));
        yaKassaSteps.payWithCard();
        urlSteps.testing().path(HISTORY).path(SALE_ID).shouldNotSeeDiff();

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
