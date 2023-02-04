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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
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

@DisplayName("Объявление - блок «Отчёт о проверке по VIN» под зарегом")
@Feature(VIN)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopDevToolsTestsModule.class)
public class VinSaleRegTest {

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

        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/OfferCarsUsedUser",
                "desktop/CarfaxOfferCarsRawNotPaid").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
        saleUrl = urlSteps.getCurrentUrl();
        basePageSteps.scrollDown(SCROLL);
        basePageSteps.onCardPage().vinReport().waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение блока под зарегом")
    public void shouldSeeHistoryBlockReg() {
        basePageSteps.onCardPage().vinReport().should(hasText("Отчёт о проверке по VIN\nОбновлён 1 февраля 2021\n" +
                "Характеристики совпадают с ПТС\nДанные о розыске и запрете на регистрацию появятся позже\n3 владельца " +
                "в ПТС\n2 записи в истории пробегов\n2 записи в истории эксплуатации\nHD фотографии\nПоиск" +
                " данных о залоге\nПоиск оценок стоимости ремонта\nПроверка на работу в такси\nЕще 10 пунктов проверки\nПоказать " +
                "бесплатный отчёт\nКупить отчёт от 99 ₽"));
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
        basePageSteps.onCardPage().billingPopup().header().waitUntil(hasText("Отчёт о проверке по VIN"));
        basePageSteps.onCardPage().billingPopup()
                .vinSwitcherSelected("Пакет из 10\u00a0отчётов\u00a0•\u00a0действует 1\u00a0год99\u00a0₽ / отчёт" +
                        "Выгода 33%990\u00a0₽").should(isDisplayed());
        basePageSteps.onCardPage().billingPopup().checkbox("Запомнить карту").hover().click();
        yaKassaSteps.payWithCard();
        urlSteps.testing().path(HISTORY).path(SALE_ID).shouldNotSeeDiff();

        shouldSeeMetrics();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Просмотр одного отчёта при наличии купленного пакета")
    public void shouldSeeSingleReportFromPackage() {
        mockRule.delete();
        mockRule.newMock().with("desktop/OfferCarsUsedUser",
                "desktop/SessionAuthUser",
                "desktop/CarfaxOfferCarsRawNotPaidQuotaLeft5",
                "desktop/CarfaxOfferCarsRawPaidDecrementQuota").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();

        basePageSteps.onCardPage().vinReport().buttonContains("Один").should(not(isDisplayed()));
        basePageSteps.onCardPage().vinReport().buttonContains("В пакете за").should(not(isDisplayed()));
        basePageSteps.onCardPage().vinReport().button("Смотреть полный отчётосталось 5\u00a0отчётов").click();
        urlSteps.testing().path(HISTORY).path(SALE_ID).shouldNotSeeDiff();
        basePageSteps.onHistoryPage().vinReport().should(isDisplayed());
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

        basePageSteps.onCardPage().vinReport().button("Купить отчёт от 99\u00a0₽").waitUntil(isDisplayed())
                .click();
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

    @Test
    @Category({Regression.class, Testing.class, Billing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Покупка одного отчёта в поп-апе")
    public void shouldBuySingleReportInPopup() {
        mockRule.with("desktop/BillingAutoruPaymentInitVinSale",
                "desktop/BillingAutoruPaymentInitVinSingleReport",
                "desktop/BillingSubscriptionsOffersHistoryReportsPricesSale",
                "desktop/BillingAutoruPaymentProcess",
                "desktop/BillingAutoruPayment",
                "desktop/CarfaxOfferCarsRawPaidDecrementQuota").update();

        basePageSteps.onCardPage().vinReport().button("Купить отчёт от 99\u00a0₽").waitUntil(isDisplayed())
                .click();
        basePageSteps.onCardPage().switchToBillingFrame();
        basePageSteps.onCardPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.onCardPage().billingPopup().header().should(hasText("Отчёт о проверке по VIN"));
        basePageSteps.onCardPage().billingPopup()
                .vinSwitcher("Один отчёт147\u00a0₽ / отчёт147\u00a0₽").click();
        basePageSteps.onCardPage().billingPopup()
                .vinSwitcherSelected("Один отчёт147\u00a0₽ / отчёт147\u00a0₽")
                .should(isDisplayed());
        basePageSteps.onCardPage().billingPopup().checkbox("Запомнить карту").hover().click();
        yaKassaSteps.payWithCard();
        urlSteps.testing().path(HISTORY).path(SALE_ID).shouldNotSeeDiff();

        shouldSeeMetrics();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Testing.class, Regression.class})
    @DisplayName("Скролл к отчёту")
    public void shouldScrollToReport() {
        urlSteps.addParam("action", "showVinReport").open();
        waitSomething(2, TimeUnit.SECONDS);
        assertThat("Не произошел скролл к отчёту", basePageSteps.getPageYOffset() > 0);
    }

    @Step("Проверяем метрики")
    private void shouldSeeMetrics() {
        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasGoal(
                seleniumMockSteps.formatGoal("CARS_OPEN_REPORT_CARD"),
                saleUrl
        )));
    }
}
