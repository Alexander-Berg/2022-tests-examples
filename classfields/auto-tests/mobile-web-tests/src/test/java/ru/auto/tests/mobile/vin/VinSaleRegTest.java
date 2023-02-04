package ru.auto.tests.mobile.vin;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.mobile.step.PaymentSteps;
import ru.auto.tests.desktop.module.MobileDevToolsTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;

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
import static ru.auto.tests.desktop.mobile.element.cardpage.VinReport.REG_FREE_REPORT_TEXT;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Объявление - блок «Проверка по VIN» под зарегом")
@Feature(VIN)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileDevToolsTestsModule.class)
public class VinSaleRegTest {

    private static final String SALE_ID = "/1076842087-f1e84/";
    private String saleUrl;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PaymentSteps paymentSteps;

    @Inject
    public SeleniumMockSteps seleniumMockSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/OfferCarsUsedUser",
                "desktop/SessionAuthUser",
                "desktop/CarfaxOfferCarsRawNotPaid").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
        saleUrl = urlSteps.getCurrentUrl();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение блока")
    public void shouldSeeVinReport() {
        basePageSteps.onCardPage().vinReport().should(hasText("Отчёт о проверке по VIN\nОбновлён 1 февраля 2021\n" +
                "Характеристики совпадают с ПТС\nДанные о розыске и запрете на регистрацию появятся позже\n3 владельца" +
                " в ПТС\n2 записи в истории пробегов\n2 записи в истории эксплуатации\nПоиск данных о залоге\n" +
                "Проверка на работу в такси\nHD фотографии\nПоиск оценок стоимости ремонта\nЕще 10 пунктов проверки" +
                "\nПоказать бесплатный отчёт\nКупить отчёт от 99 ₽"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Показать бесплатный отчёт»")
    public void shouldClickShowFreeReportButton() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().vinReport()
                .buttonContains("Показать бесплатный отчёт"));
        basePageSteps.onCardPage().vinReport().waitUntil(hasText(REG_FREE_REPORT_TEXT));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по записи")
    public void shouldClickRecord() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().vinReport()
                .button("Характеристики совпадают с\u00a0ПТС"));
        basePageSteps.onCardPage().vinReport().waitUntil(hasText(REG_FREE_REPORT_TEXT));
    }

    @Test
    @Category({Regression.class, Testing.class, Billing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Покупка пакета из 10 отчётов по кнопке «Купить отчёт от»")
    public void shouldBuy10ReportsPackage() {
        basePageSteps.scrollDown(300);
        basePageSteps.onCardPage().vinReport().waitUntil(isDisplayed());

        mockRule.delete();
        mockRule.newMock().with("desktop/OfferCarsUsedUser",
                "desktop/SessionAuthUser",
                "desktop/BillingSubscriptionsOffersHistoryReportsPricesSale",
                "mobile/BillingAutoruPaymentInitVinSale",
                "mobile/BillingAutoruPaymentProcessSaleVin",
                "desktop/BillingAutoruPayment",
                "desktop/CarfaxOfferCarsRawPaidDecrementQuota").post();

        basePageSteps.scrollAndClick(basePageSteps.onCardPage().vinReport().button("Купить отчёт от 99\u00a0₽"));
        basePageSteps.onCardPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.switchToPaymentMethodsFrame();
        basePageSteps.onCardPage().paymentMethodsFrameContent().should(hasText("Отчёт о проверке по VIN\n1 отчёт\n" +
                "147 ₽ / шт.\n147 ₽\n5 отчётов\n • \nдействует 1 год\n120 ₽ / шт.\n599 ₽\n10 отчётов\n • \n" +
                "действует 1 год\n99 ₽ / шт.\n−33%\n990 ₽\nБанковская карта\nСбербанк Онлайн\nЮMoney\n" +
                "QIWI Кошелек\nWebmoney\nОплатить 990 ₽\nСовершая платеж, вы соглашаетесь с условиями Оферты"));
        basePageSteps.switchToDefaultFrame();
        basePageSteps.clickPayButton();
        paymentSteps.payByCard();
        urlSteps.testing().path(HISTORY).path(SALE_ID).shouldNotSeeDiff();

        shouldSeeMetrics();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Просмотр одного отчёта при наличии купленного пакета")
    public void shouldSeeSingleReportFromPackage() {
        basePageSteps.scrollDown(300);
        basePageSteps.onCardPage().vinReport().waitUntil(isDisplayed());

        mockRule.delete();
        mockRule.newMock().with("desktop/OfferCarsUsedUser",
                "desktop/SessionAuthUser",
                "desktop/CarfaxOfferCarsRawNotPaidQuotaLeft5",
                "desktop/CarfaxOfferCarsRawPaidDecrementQuota").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();

        basePageSteps.onCardPage().vinReport().buttonContains("Один").should(not(isDisplayed()));
        basePageSteps.onCardPage().vinReport().buttonContains("В пакете за").should(not(isDisplayed()));
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().vinReport()
                .button("Смотреть полный отчётосталось 5\u00a0отчётов"));
        urlSteps.testing().path(HISTORY).path(SALE_ID).shouldNotSeeDiff();
        basePageSteps.onHistoryPage().vinReport().should(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class, Billing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Покупка пакета из 5 отчётов в поп-апе")
    public void shouldBuy5ReportsPackageInPopup() {
        basePageSteps.scrollDown(300);
        basePageSteps.onCardPage().vinReport().waitUntil(isDisplayed());

        mockRule.delete();
        mockRule.newMock().with("desktop/OfferCarsUsedUser",
                "desktop/SessionAuthUser",
                "desktop/BillingSubscriptionsOffersHistoryReportsPricesSale",
                "mobile/BillingAutoruPaymentInitVinSale",
                "mobile/BillingAutoruPaymentInitVin5ReportsPackage",
                "mobile/BillingAutoruPaymentProcessSaleVin",
                "desktop/BillingAutoruPayment",
                "desktop/CarfaxOfferCarsRawPaidDecrementQuota").post();

        basePageSteps.scrollAndClick(basePageSteps.onCardPage().vinReport().button("Купить отчёт от 99\u00a0₽"));
        basePageSteps.onCardPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.selectVinPackage("5\u00a0отчётов\u00a0•\u00a0действует 1\u00a0год120\u00a0₽ / шт." +
                "599\u00a0₽");
        basePageSteps.selectPaymentMethod("Банковская карта");
        basePageSteps.clickPayButton();
        paymentSteps.payByCard();
        urlSteps.testing().path(HISTORY).path(SALE_ID).shouldNotSeeDiff();

        shouldSeeMetrics();
    }

    @Test
    @Category({Regression.class, Testing.class, Billing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Покупка отчёта через Сбербанк")
    public void shouldBuySingleReportViaSberbank() {
        basePageSteps.scrollDown(300);
        basePageSteps.onCardPage().vinReport().waitUntil(isDisplayed());

        mockRule.delete();
        mockRule.newMock().with("desktop/OfferCarsUsedUser",
                "desktop/SessionAuthUser",
                "desktop/BillingSubscriptionsOffersHistoryReportsPricesSale",
                "mobile/BillingAutoruPaymentInitVinSale",
                "mobile/BillingAutoruPaymentProcessSaleSberbank",
                "desktop/BillingAutoruPayment",
                "desktop/CarfaxOfferCarsRawPaidDecrementQuota").post();

        basePageSteps.scrollAndClick(basePageSteps.onCardPage().vinReport().button("Купить отчёт от 99\u00a0₽"));
        basePageSteps.selectPaymentMethod("Сбербанк Онлайн");
        basePageSteps.clickPayButton();
        basePageSteps.switchToPaymentMethodsFrame();
        paymentSteps.onPaymentPage().input("Номер телефона", "9111111111");
        paymentSteps.onPaymentPage().button("Продолжить").click();
        basePageSteps.switchToDefaultFrame();
        waitSomething(5, TimeUnit.SECONDS);
        urlSteps.testing().path(HISTORY).path(SALE_ID).shouldNotSeeDiff();

        shouldSeeMetrics();
    }

    @Test
    @Category({Regression.class, Testing.class, Billing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Покупка пакета из 50 отчётов по кнопке «Показать полный VIN и госномер»")
    public void shouldBuy50ReportsPackageViaShowReportButton() {
        basePageSteps.scrollDown(300);
        basePageSteps.onCardPage().vinReport().waitUntil(isDisplayed());

        mockRule.delete();
        mockRule.newMock().with("desktop/OfferCarsUsedUser",
                "desktop/SessionAuthUser",
                "desktop/BillingSubscriptionsOffersHistoryReportsPricesSale",
                "mobile/BillingAutoruPaymentInitVinShowReportButtonSale",
                "mobile/BillingAutoruPaymentProcessSaleVin",
                "desktop/BillingAutoruPayment",
                "desktop/CarfaxOfferCarsRawPaidDecrementQuota").post();

        basePageSteps.scrollAndClick(basePageSteps.onCardPage()
                .button("Показать полный VIN и госномер от 99\u00a0₽"));
        basePageSteps.onCardPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.switchToPaymentMethodsFrame();
        basePageSteps.onCardPage().paymentMethodsFrameContent().should(hasText("Отчёт о проверке по VIN\n1 отчёт\n" +
                "147 ₽ / шт.\n147 ₽\n5 отчётов\n • \nдействует 1 год\n120 ₽ / шт.\n599 ₽\n10 отчётов\n • \n" +
                "действует 1 год\n99 ₽ / шт.\n−33%\n990 ₽\nБанковская карта\nСбербанк Онлайн\nЮMoney\n" +
                "QIWI Кошелек\nWebmoney\nОплатить 990 ₽\nСовершая платеж, вы соглашаетесь с условиями Оферты"));
        basePageSteps.switchToDefaultFrame();
        basePageSteps.clickPayButton();
        paymentSteps.payByCard();
        urlSteps.testing().path(HISTORY).path(SALE_ID).shouldNotSeeDiff();

        shouldSeeMetrics();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Testing.class, Regression.class})
    @DisplayName("Скролл к отчёту")
    public void shouldScrollToReport() {
        urlSteps.addParam("action", "showVinReport").open();
        waitSomething(1, TimeUnit.SECONDS);
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
