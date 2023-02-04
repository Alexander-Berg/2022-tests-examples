package ru.auto.tests.mobile.vin;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.mobile.step.PaymentSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import static ru.auto.tests.desktop.consts.AutoruFeatures.VIN;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.HISTORY;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mobile.element.cardpage.VinReport.OWNER_FREE_REPORT_TEXT;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Объявление - блок «Проверка по VIN» под владельцем")
@Feature(VIN)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class VinSaleOwnerTest {

    private static final String SALE_ID = "/1076842087-f1e84/";

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

    @Before
    public void before() {
        mockRule.newMock().with("desktop/OfferCarsUsedUserOwner",
                "desktop/SessionAuthUser",
                "desktop/CarfaxOfferCarsRawNotPaidOwner").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение блока")
    public void shouldSeeVinReport() {
        basePageSteps.onCardPage().vinReport().should(hasText("Отчёт о проверке по VIN\nОбновлён 1 февраля 2021\n" +
                "Характеристики совпадают с ПТС\nДанные о розыске и запрете на регистрацию появятся позже\n3 " +
                "владельца в ПТС\nДанные о залоге не найдены\n2 записи в истории пробегов\n2 записи в истории " +
                "эксплуатации\nПроверка на работу в такси\nHD фотографии\nПоиск оценок стоимости ремонта\nЕще 10 " +
                "пунктов проверки\nПоказать бесплатный отчёт\nКупить отчёт от 99 ₽"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Показать бесплатный отчёт»")
    public void shouldClickShowFreeReportButton() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().vinReport()
                .buttonContains("Показать бесплатный отчёт"));
        basePageSteps.onCardPage().vinReport().should(hasText(OWNER_FREE_REPORT_TEXT));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по записи")
    public void shouldClickRecord() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().vinReport()
                .button("Характеристики совпадают с\u00a0ПТС"));
        basePageSteps.onCardPage().vinReport().should(hasText(OWNER_FREE_REPORT_TEXT));
    }


    @Test
    @Category({Regression.class, Testing.class, Billing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Покупка пакета из 10 отчётов по кнопке «Купить отчёт от»")
    public void shouldBuy10ReportsPackage() {
        basePageSteps.scrollDown(300);
        basePageSteps.onCardPage().vinReport().waitUntil(isDisplayed());

        mockRule.delete();
        mockRule.newMock().with("desktop/OfferCarsUsedUserOwner",
                "desktop/SessionAuthUser",
                "desktop/BillingSubscriptionsOffersHistoryReportsPricesSale",
                "mobile/BillingAutoruPaymentInitVinSale",
                "mobile/BillingAutoruPaymentProcessSaleVin",
                "desktop/BillingAutoruPayment",
                "desktop/CarfaxOfferCarsRawPaidDecrementQuota").post();

        basePageSteps.scrollAndClick(basePageSteps.onCardPage().vinReport().button("Купить отчёт от 99\u00a0₽"));
        basePageSteps.onCardPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.selectPaymentMethod("Банковская карта");
        basePageSteps.clickPayButton();
        paymentSteps.payByCard();
        urlSteps.testing().path(HISTORY).path(SALE_ID).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class, Billing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Покупка пакета из 5 отчётов в поп-апе")
    public void shouldBuy5ReportsPackageInPopup() {
        basePageSteps.scrollDown(300);
        basePageSteps.onCardPage().vinReport().waitUntil(isDisplayed());

        mockRule.delete();
        mockRule.newMock().with("desktop/OfferCarsUsedUserOwner",
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
    }
}
