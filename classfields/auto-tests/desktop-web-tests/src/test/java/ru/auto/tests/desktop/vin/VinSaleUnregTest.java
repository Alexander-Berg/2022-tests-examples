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
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
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
import static ru.auto.tests.desktop.step.CookieSteps.FORCE_DISABLE_TRUST;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Объявление - блок «Отчёт о проверке по VIN» под незарегом")
@Feature(VIN)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class VinSaleUnregTest {

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
    private YaKassaSteps yaKassaSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() {
        cookieSteps.setExpFlags(FORCE_DISABLE_TRUST);

        mockRule.newMock().with("desktop/OfferCarsUsedUser",
                "desktop/CarfaxOfferCarsRawNotPaid",
                "desktop/OfferCarsStatsEmpty",
                "desktop/OfferCarsCallsStatsEmpty",
                "desktop/ApiV2CounterAutoRuCurrentShows").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение блока под незарегом")
    public void shouldSeeVinReport() {
        basePageSteps.onCardPage().vinReport().should(hasText("Отчёт о проверке по VIN\nОбновлён 1 февраля 2021\n" +
                "Характеристики совпадают с ПТС\nДанные о розыске и запрете на регистрацию появятся позже\n3 владельца " +
                "в ПТС\n2 записи в истории пробегов\n2 записи в истории эксплуатации\nHD фотографии\nПоиск " +
                "данных о залоге\nПоиск оценок стоимости ремонта\nПроверка на работу в такси\nЕще 10 пунктов проверки\nПоказать " +
                "бесплатный отчёт\nКупить полный отчёт"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Покупка пакета из 10 отчётов по кнопке «Купить полный отчёт»")
    public void shouldBuy10ReportsPackage() {
        basePageSteps.onCardPage().vinReport().button("Купить полный отчёт").click();
        authorize();
        waitSomething(3, TimeUnit.SECONDS);

        mockRule.delete();
        mockRule.newMock().with("desktop/OfferCarsUsedUser",
                "desktop/SessionAuthUser",
                "desktop/BillingAutoruPaymentProcess",
                "desktop/BillingAutoruPayment",
                "desktop/CarfaxOfferCarsRawPaid").post();

        basePageSteps.onCardPage().switchToBillingFrame();
        basePageSteps.onCardPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.onCardPage().billingPopup().header().should(hasText("Отчёт о проверке по VIN"));
        basePageSteps.onCardPage().billingPopup().checkbox("Запомнить карту").hover().click();
        yaKassaSteps.payWithCard();
        urlSteps.testing().path(HISTORY).path(SALE_ID).shouldNotSeeDiff();
        basePageSteps.onHistoryPage().vinReport().waitUntil(isDisplayed());
        basePageSteps.onHistoryPage().vinReport().status().waitUntil(isDisplayed())
                .should(hasText("Проверено 8 из 9 источников\nМы сообщим вам, как только отчёт будет полностью готов"));
        basePageSteps.onHistoryPage().vinReport().contents().should(hasText("Содержание отчёта\nДанные из ПТС\n" +
                "Есть расхождения в характеристиках\nНаличие ограничений\nНет записей\nВладельцы по ПТС\n4 владельца\n" +
                "Участие в ДТП\n1 ДТП\nОтзывные кампании\nНет записей\n" +
                "Размещения на Авто.ру\nЕщё 1 объявление\nИстория пробегов\nЭто объявление + 1 запись\n" +
                "История эксплуатации\nЭто объявление + 2 записи\nОценка стоимости\n~ 702 736 ₽\n" +
                "Транспортный налог\n5 250 ₽ в год"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по VIN в характеристиках")
    public void shouldClickFeatureVin() {
        basePageSteps.onCardPage().features().feature("VIN").hover().click();
        shouldSeeAuthPopup();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по госномеру в характеристиках")
    public void shouldClickFeatureLicensePlate() {
        basePageSteps.onCardPage().features().feature("Госномер").hover().click();
        shouldSeeAuthPopup();
    }

    @Step("Должен появиться поп-ап авторизации")
    private void shouldSeeAuthPopup() {
        basePageSteps.onCardPage().authPopup().waitUntil(isDisplayed());
        basePageSteps.onCardPage().switchToAuthPopupFrame();
        basePageSteps.onCardPage().authPopupFrame().input("Телефон или электронная почта").waitUntil(isDisplayed());
        basePageSteps.onCardPage().authPopupFrame().button("Продолжить").waitUntil(isDisplayed());
    }

    @Step("Авторизуемся")
    private void authorize() {
        mockRule.delete();
        mockRule.newMock().with("desktop/AuthLoginOrRegisterRedirect",
                "desktop/UserConfirm",
                "desktop/SessionAuthUser",
                "desktop/OfferCarsUsedUser",
                "desktop/CarfaxOfferCarsRawNotPaidDecrementQuota",
                "desktop/BillingSubscriptionsOffersHistoryReportsPricesSale",
                "desktop/BillingAutoruPaymentInitVinSaleUnreg").post();

        basePageSteps.onCardPage().switchToAuthPopupFrame();
        basePageSteps.onCardPage().authPopupFrame().input("Телефон или электронная почта", "9111111111");
        basePageSteps.onCardPage().authPopupFrame().button("Продолжить").click();
        basePageSteps.onCardPage().authPopupFrame().input("Код из смс", "1234");
        basePageSteps.switchToDefaultFrame();
    }
}
