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

@DisplayName("Покупка истории по кнопке «Посмотреть» в блоке «История автомобиля» под владельцем")
@Feature(VIN)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopDevToolsTestsModule.class)
public class VinSaleOwnerTest {

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
    private CookieSteps cookieSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    public SeleniumMockSteps seleniumMockSteps;

    @Before
    public void before() {
        cookieSteps.setCookieForBaseDomain("promo_popup_history_seller_closed", "true");
        cookieSteps.setExpFlags(FORCE_DISABLE_TRUST);

        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/OfferCarsUsedUserOwner",
                "desktop/CarfaxOfferCarsRawNotPaid").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
        saleUrl = urlSteps.getCurrentUrl();
        basePageSteps.scrollDown(SCROLL);
        basePageSteps.onCardPage().vinReport().waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class, Billing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Покупка пакета из 10 отчётов по кнопке «Купить отчёт от»")
    public void shouldBuy10ReportsPackage() {
        mockRule.with("desktop/BillingSubscriptionsOffersHistoryReportsPricesSale",
                "desktop/BillingAutoruPaymentInitVinSale",
                "desktop/BillingAutoruPaymentProcess",
                "desktop/BillingAutoruPayment",
                "desktop/CarfaxOfferCarsRawPaidDecrementQuota").update();

        basePageSteps.onCardPage().vinReport().button("Купить отчёт от 99\u00a0₽").waitUntil(isDisplayed())
                .click();
        basePageSteps.onCardPage().switchToBillingFrame();
        basePageSteps.onCardPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.onCardPage().billingPopup().header().should(hasText("Отчёт о проверке по VIN"));
        basePageSteps.onCardPage().billingPopup()
                .vinSwitcherSelected("Пакет из 10\u00a0отчётов\u00a0•\u00a0действует 1\u00a0год99\u00a0₽ / отчёт" +
                        "Выгода 33%990\u00a0₽").should(isDisplayed());
        basePageSteps.onCardPage().billingPopup().checkbox("Запомнить карту").hover().click();
        yaKassaSteps.payWithCard();
        urlSteps.testing().path(HISTORY).path(SALE_ID).shouldNotSeeDiff();

        shouldSeeMetrics();
    }

    @Test
    @Category({Regression.class, Testing.class, Billing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Покупка пакета из 10 отчётов по клику на VIN в характеристиках")
    public void shouldBuy10ReportsPackageViaFeatureVinButton() {
        mockRule.with("desktop/BillingSubscriptionsOffersHistoryReportsPricesSale",
                "desktop/BillingAutoruPaymentInitVinCardInfo",
                "desktop/BillingAutoruPaymentProcess",
                "desktop/BillingAutoruPayment",
                "desktop/CarfaxOfferCarsRawPaidDecrementQuota").update();

        basePageSteps.onCardPage().features().feature("VIN").hover().click();
        basePageSteps.onCardPage().switchToBillingFrame();
        basePageSteps.onCardPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.onCardPage().billingPopup().header().should(hasText("Отчёт о проверке по VIN"));
        basePageSteps.onCardPage().billingPopup()
                .vinSwitcherSelected("Пакет из 10\u00a0отчётов\u00a0•\u00a0действует 1\u00a0год99\u00a0₽ / отчёт" +
                        "Выгода 33%990\u00a0₽").should(isDisplayed());
        basePageSteps.onCardPage().billingPopup().checkbox("Запомнить карту").hover().click();
        yaKassaSteps.payWithCard();
        urlSteps.testing().path(HISTORY).path(SALE_ID).shouldNotSeeDiff();

        shouldSeeMetrics();
    }

    @Test
    @Category({Regression.class, Testing.class, Billing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Покупка пакета из 5 отчётов в поп-апе")
    public void shouldBuy5ReportsPackageInPopup() {
        mockRule.with("desktop/BillingAutoruPaymentInitVinSale",
                "desktop/BillingAutoruPaymentInitVin5ReportsPackage",
                "desktop/BillingSubscriptionsOffersHistoryReportsPricesSale",
                "desktop/BillingAutoruPaymentProcess",
                "desktop/BillingAutoruPayment",
                "desktop/CarfaxOfferCarsRawPaidDecrementQuota").update();

        basePageSteps.onCardPage().vinReport().button("Купить отчёт от 99\u00a0₽").click();
        basePageSteps.onCardPage().switchToBillingFrame();
        basePageSteps.onCardPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.onCardPage().billingPopup().header().should(hasText("Отчёт о проверке по VIN"));
        basePageSteps.onCardPage().billingPopup()
                .vinSwitcher("Пакет из 5\u00a0отчётов\u00a0•\u00a0действует 1\u00a0год120\u00a0₽ / отчёт599\u00a0₽")
                .click();
        basePageSteps.onCardPage().billingPopup()
                .vinSwitcherSelected("Пакет из 5\u00a0отчётов\u00a0•\u00a0действует 1\u00a0год120\u00a0₽ / отчёт599\u00a0₽")
                .click();
        basePageSteps.onCardPage().billingPopup().checkbox("Запомнить карту").hover().click();
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
