package ru.auto.tests.mobile.vas;

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
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import static ru.auto.tests.desktop.consts.AutoruFeatures.VAS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Покупка VAS на карточке")
@Feature(VAS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class VasSaleVipCarsTest {

    private static final String SALE_ID = "/1076842087-f1e84/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private PaymentSteps paymentSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/OfferCarsUsedUserOwnerWithVip",
                "desktop/SessionAuthUser",
                "mobile/BillingAutoruPaymentInitVip",
                "mobile/BillingAutoruPaymentProcessSaleCars",
                "desktop/BillingAutoruPayment").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();

        mockRule.overwriteStub(0, "desktop/OfferCarsUsedUserOwnerWithVipPaid");
    }

    @Test
    @Category({Regression.class, Testing.class, Billing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Покупка услуги")
    public void shouldBuyVas() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().vas().service("VIP").buyButton());
        buyVas();
    }

    @Test
    @Category({Regression.class, Testing.class, Billing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Покупка услуги в поп-апе")
    public void shouldBuyVasInPopup() {
        basePageSteps.openVasPopup("VIP");
        buyVas();
    }

    @Step("Покупаем VAS")
    private void buyVas() {
        basePageSteps.clickPayButton();
        paymentSteps.payByCard();
        paymentSteps.waitForSuccessMessage();
        basePageSteps.onCardPage().notifier().waitUntil(hasText("Опция успешно активирована"));
        basePageSteps.onCardPage().vas().waitUntil(hasText("VIP\nMAX просмотров · Включено 4 опции · 60 дней\n" +
                "до окончания\nПоказ в Историях\nДействует 3 дня\n834 ₽"));
        basePageSteps.onCardPage().vas().activeService("VIP").click();
        basePageSteps.onCardPage().vasPopup().title().should(hasText("VIP"));
        basePageSteps.onCardPage().vasPopup().multiplier().should(hasText("MAX просмотров"));
        basePageSteps.onCardPage().vasPopup().description().should(hasText("Это супер-комбо — все мощности Авто.ру будут " +
                "использованы для продажи вашего автомобиля. Объявление будет размещено в специальном блоке " +
                "вверху страниц, выделено цветом и самое главное — каждый день будет автоматически подниматься " +
                "на первое место до конца размещения. Действует 60 дней."));
        basePageSteps.onCardPage().vasPopup().status().should(hasText("Активно"));
    }
}
