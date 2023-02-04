package ru.auto.tests.desktop.lk.sales.reseller;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.YaKassaSteps;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.HISTORY;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.RESELLER;
import static ru.auto.tests.desktop.step.CookieSteps.FORCE_DISABLE_TRUST;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Объявление с проверенным VIN")
@Epic(AutoruFeatures.LK)
@Feature(AutoruFeatures.MY_OFFERS_RESELLER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class CheckedVinCarsOfferTest {

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

        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop-lk/UserFavoriteReseller",
                "desktop-lk/reseller/UserOffersCarsWithCheckedVin",
                "desktop/CarfaxOfferCarsRawNotPaid").post();

        urlSteps.testing().path(MY).path(RESELLER).path(CARS).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Поп-ап с историей по VIN")
    public void shouldSeeVinHistoryPopup() {
        basePageSteps.onLkResellerSalesPage().getSale(0).mainInfoColumn().vin().hover();
        basePageSteps.onLkResellerSalesPage().popup().waitUntil(isDisplayed())
                .should(hasText("Проверка по VIN\nX9FHXXEEDHAG40989 от 1 февраля 2021 г.\n" +
                        "Характеристики совпадают с ПТС\nДанные о розыске и запрете на регистрацию появятся позже\n" +
                        "3 владельца в ПТС\nПолный отчёт за 99 ₽ / шт\nВместо 499 ₽"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Покупаем историю со страницы ЛК")
    public void buyVinHistory() {
        mockRule.with("desktop-lk/reseller/BillingAutoruPaymentInitVinHistory",
                "desktop-lk/reseller/BillingAutoruPaymentProcess",
                "desktop-lk/BillingAutoruPayment",
                "desktop/CarfaxOfferCarsRawPaidDecrementQuota").update();

        basePageSteps.onLkResellerSalesPage().getSale(0).mainInfoColumn().vin().hover();
        basePageSteps.onLkResellerSalesPage().popup().buttonContains("Полный отчёт").click();
        basePageSteps.onLkResellerSalesPage().switchToBillingFrame();
        basePageSteps.onLkResellerSalesPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.onLkResellerSalesPage().billingPopup().checkbox("Запомнить карту").click();
        basePageSteps.onLkResellerSalesPage().billingPopup().header().waitUntil(hasText("Отчёт о проверке по VIN"));
        basePageSteps.onLkResellerSalesPage().billingPopup().priceHeader().waitUntil(hasText("990 \u20BD"));
        yaKassaSteps.payWithCard();
        basePageSteps.onLkResellerSalesPage().billingPopupFrame().waitUntil(not(isDisplayed()));
        urlSteps.testing().path(HISTORY).path("/1076842087-f1e84/").shouldNotSeeDiff();
    }
}
