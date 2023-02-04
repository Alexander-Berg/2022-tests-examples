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
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.mobile.step.PaymentSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.AutoruFeatures.VIN;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.HISTORY;
import static ru.auto.tests.desktop.consts.Pages.LOGIN;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_AUTH;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.lanwen.diff.uri.core.util.URLCoder.encode;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Объявление - блок «Проверка по VIN» под незарегом")
@Feature(VIN)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class VinSaleUnregTest {

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
    private PaymentSteps paymentSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/OfferCarsUsedUser",
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
                "Характеристики совпадают с ПТС\nДанные о розыске и запрете на регистрацию появятся позже\n3 " +
                "владельца в ПТС\n2 записи в истории пробегов\n2 записи в истории эксплуатации\nПоиск данных о " +
                "залоге\nПроверка на работу в такси\nHD фотографии\nПоиск оценок стоимости ремонта\nЕще 10 пунктов " +
                "проверки\nПоказать бесплатный отчёт\nКупить полный отчёт"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Показать бесплатный отчёт»")
    public void shouldClickShowFreeReportButton() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().vinReport().button("Показать бесплатный отчёт"));
        urlSteps.subdomain(SUBDOMAIN_AUTH).path(LOGIN)
                .addParam("r", encode(format("%s", saleUrl))).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по записи")
    public void shouldClickRecord() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().vinReport()
                .button("Характеристики совпадают с\u00a0ПТС"));
        urlSteps.subdomain(SUBDOMAIN_AUTH).path(LOGIN)
                .addParam("r", encode(format("%s", saleUrl))).shouldNotSeeDiff();
    }


    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Покупка пакета из 10 отчётов по кнопке «Купить полный отчёт»")
    public void shouldBuy10ReportsPackage() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().vinReport().button("Купить полный отчёт"));
        urlSteps.subdomain(SUBDOMAIN_AUTH).path(LOGIN)
                .addParam("r",
                        encode(format("%s?from=api_m_card_unauthorized&vinHistoryButton=CardVinReportBundleButton",
                                saleUrl)))
                .addParam("from", "api_m_card_unauthorized").shouldNotSeeDiff();

        authorizeAndBuy10ReportsPackage();
        urlSteps.fromUri(saleUrl).addParam("from", "api_m_card_unauthorized")
                .addParam("vinHistoryButton", "CardVinReportBundleButton")
                .shouldNotSeeDiff();
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
    }

    @Step("Авторизуемся и покупаем пакет из 10 отчётов")
    private void authorizeAndBuy10ReportsPackage() {
        mockRule.delete();
        mockRule.newMock().with("desktop/AuthLoginOrRegisterRedirect",
                "desktop/UserConfirm",
                "desktop/SessionAuthUser",
                "desktop/OfferCarsUsedUser",
                "desktop/CarfaxOfferCarsRawNotPaid",
                "desktop/BillingSubscriptionsOffersHistoryReportsPricesSale",
                "mobile/BillingAutoruPaymentInitVinSale",
                "mobile/BillingAutoruPaymentProcessSaleVin",
                "desktop/BillingAutoruPayment",
                "desktop/CarfaxOfferCarsRawPaidDecrementQuota").post();

        basePageSteps.onAuthPage().input("Номер телефона").sendKeys("9111111111");
        basePageSteps.onAuthPage().input("Код из смс", "1234");
    }
}
